package com.trasua.support;

import com.trasua.domain.Member;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Creates a VietQR image URL while keeping the technical bank BIN out of the user form. */
public final class QrImageUrlFactory {
    private static final Map<String, String> BANK_BINS = Map.ofEntries(
            Map.entry("vietcombank", "970436"), Map.entry("vcb", "970436"),
            Map.entry("vietinbank", "970415"), Map.entry("viettinbank", "970415"),
            Map.entry("bidv", "970418"),
            Map.entry("agribank", "970405"), Map.entry("mb", "970422"),
            Map.entry("mbbank", "970422"), Map.entry("techcombank", "970407"),
            Map.entry("acb", "970416"), Map.entry("vpbank", "970432"),
            Map.entry("tpbank", "970423"), Map.entry("vib", "970441"),
            Map.entry("hdbank", "970437"), Map.entry("sacombank", "970403"),
            Map.entry("shb", "970443"), Map.entry("ocb", "970448"),
            Map.entry("msb", "970426"), Map.entry("eximbank", "970431"),
            Map.entry("seabank", "970440"), Map.entry("lpbank", "970449"),
            Map.entry("lienvietpostbank", "970449"), Map.entry("pvcombank", "970412"),
            Map.entry("namabank", "970428"), Map.entry("abbank", "970425"),
            Map.entry("ncb", "970419"), Map.entry("bacabank", "970409"),
            Map.entry("vietabank", "970427"), Map.entry("baovietbank", "970438"),
            Map.entry("kienlongbank", "970452")
    );

    private QrImageUrlFactory() {
    }

    public static String forTransfer(Member recipient, long amount, String content) {
        if (hasText(recipient.getQrCodeUrl())) {
            return recipient.getQrCodeUrl();
        }
        String bankBin = hasText(recipient.getBankBin())
                ? recipient.getBankBin().replaceAll("[^0-9A-Za-z]", "")
                : BANK_BINS.get(normalizeBankName(recipient.getBankName()));
        if (!hasText(bankBin) || !hasText(recipient.getAccountNumber())) {
            return null;
        }
        String account = recipient.getAccountNumber().replaceAll("\\s+", "");
        String accountName = recipient.getAccountName() == null ? "" : recipient.getAccountName();
        return "https://img.vietqr.io/image/" + bankBin + "-" + account + "-compact2.png"
                + "?amount=" + amount
                + "&addInfo=" + encode(content)
                + "&accountName=" + encode(accountName);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeBankName(String value) {
        if (!hasText(value)) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
