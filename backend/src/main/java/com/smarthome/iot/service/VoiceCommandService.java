package com.smarthome.iot.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smarthome.iot.domain.Device;

/**
 * Chuyển câu lệnh tiếng Việt (kết quả nhận dạng giọng nói từ trình duyệt) thành lệnh ON/OFF cho thiết bị.
 * Ví dụ: "bật đèn 1", "tắt đèn một", "mở quạt phòng khách", "tắt tất cả".
 */
@Service
public class VoiceCommandService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCommandService.class);

    private static final Set<String> ON_WORDS = Set.of("bat", "mo");
    private static final Set<String> OFF_WORDS = Set.of("tat", "dong");
    private static final Set<String> ALL_WORDS = Set.of("het", "toan");
    // Từ chung chung không dùng để xác định thiết bị cụ thể
    private static final Set<String> GENERIC_WORDS = Set.of("thiet", "bi", "cac", "nha", "trong", "bo", "do", "dien",
            "giup", "toi", "hay", "lam", "on", "vui", "long", "di", "nhe", "cho");
    private static final Map<String, String> NUMBER_WORDS = Map.of(
            "mot", "1", "hai", "2", "ba", "3", "bon", "4", "nam", "5", "sau", "6", "bay", "7", "tam", "8", "chin", "9");

    public record Result(boolean success, String message, String status, List<Device> devices) {
        static Result fail(String message) {
            return new Result(false, message, null, List.of());
        }
    }

    private final DeviceService deviceService;

    public VoiceCommandService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * @throws MqttUnavailableException nếu không gửi được lệnh xuống gateway
     */
    public Result execute(String text) {
        if (text == null || text.isBlank()) {
            return Result.fail("Không nghe thấy lệnh nào");
        }

        List<String> tokens = tokenize(text);
        boolean allPhrase = removeTatCa(tokens);
        boolean all = allPhrase || tokens.stream().anyMatch(ALL_WORDS::contains);

        boolean on = tokens.stream().anyMatch(ON_WORDS::contains);
        boolean off = tokens.stream().anyMatch(OFF_WORDS::contains);
        if (allPhrase && !on) {
            off = true; // "tắt tất cả" / "tất cả đèn": không có từ bật -> tắt
        }
        if (on == off) {
            return Result.fail(on ? "Lệnh mâu thuẫn, hãy nói bật hoặc tắt" : "Không hiểu lệnh, hãy nói \"bật\" hoặc \"tắt\" kèm tên thiết bị");
        }
        String status = on ? "ON" : "OFF";

        List<Device> targets = resolveTargets(tokens, all);
        if (targets.isEmpty()) {
            return Result.fail("Không tìm thấy thiết bị trong câu lệnh \"" + text.trim() + "\"");
        }
        if (targets.size() > 1 && !all) {
            String names = targets.stream().map(Device::getName).collect(Collectors.joining(", "));
            return Result.fail("Có nhiều thiết bị phù hợp (" + names + "), hãy nói rõ hơn");
        }

        List<Device> changed = new ArrayList<>();
        for (Device device : targets) {
            Device saved = deviceService.setStatus(device.getId(), status);
            if (saved != null) {
                changed.add(saved);
            }
        }
        if (changed.isEmpty()) {
            return Result.fail("Không cập nhật được thiết bị nào");
        }

        String names = changed.stream().map(Device::getName).collect(Collectors.joining(", "));
        log.info("Voice command '{}' -> {} {}", text, status, names);
        return new Result(true, ("ON".equals(status) ? "Đã bật " : "Đã tắt ") + names, status, changed);
    }

    private List<Device> resolveTargets(List<String> tokens, boolean all) {
        List<Device> devices = deviceService.getAllDevice();
        List<String> rest = tokens.stream()
                .filter(t -> !ON_WORDS.contains(t) && !OFF_WORDS.contains(t) && !ALL_WORDS.contains(t) && !GENERIC_WORDS.contains(t))
                .collect(Collectors.toList());

        // 1. Tên thiết bị xuất hiện nguyên vẹn trong câu: lấy tên dài nhất
        int bestLen = 0;
        List<Device> exact = new ArrayList<>();
        for (Device d : devices) {
            List<String> nameTokens = tokenize(d.getName());
            if (nameTokens.isEmpty() || !containsSequence(tokens, nameTokens)) {
                continue;
            }
            if (nameTokens.size() > bestLen) {
                bestLen = nameTokens.size();
                exact.clear();
            }
            if (nameTokens.size() == bestLen) {
                exact.add(d);
            }
        }
        if (!exact.isEmpty()) {
            return disambiguateByRoom(exact, tokens);
        }

        // 2. "tắt tất cả đèn": lọc thiết bị có tên chứa từ còn lại; không còn từ nào -> tất cả thiết bị
        if (all) {
            if (rest.isEmpty()) {
                return devices;
            }
            return devices.stream()
                    .filter(d -> tokenize(d.getName()).stream().anyMatch(rest::contains))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /** Nhiều thiết bị trùng tên (vd "Đèn" ở nhiều phòng): ưu tiên thiết bị có tên phòng xuất hiện trong câu. */
    private List<Device> disambiguateByRoom(List<Device> candidates, List<String> tokens) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        List<Device> byRoom = candidates.stream()
                .filter(d -> d.getRoom() != null && d.getRoom().getName() != null
                        && containsSequence(tokens, tokenize(d.getRoom().getName())))
                .collect(Collectors.toList());
        return byRoom.isEmpty() ? candidates : byRoom;
    }

    /** Bỏ cặp "tat ca" (nghĩa "tất cả") khỏi token để "tat" trong đó không bị tính là lệnh tắt. */
    private boolean removeTatCa(List<String> tokens) {
        for (int i = 0; i + 1 < tokens.size(); i++) {
            if ("tat".equals(tokens.get(i)) && "ca".equals(tokens.get(i + 1))) {
                tokens.remove(i + 1);
                tokens.remove(i);
                return true;
            }
        }
        return false;
    }

    private boolean containsSequence(List<String> haystack, List<String> needle) {
        if (needle.isEmpty() || needle.size() > haystack.size()) {
            return false;
        }
        for (int i = 0; i + needle.size() <= haystack.size(); i++) {
            if (haystack.subList(i, i + needle.size()).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    /** Chữ thường, bỏ dấu, bỏ ký tự đặc biệt, đổi số đếm bằng chữ ("một") thành chữ số. */
    static List<String> tokenize(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        String s = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (s.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(s.split(" "))
                .map(t -> NUMBER_WORDS.getOrDefault(t, t))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
