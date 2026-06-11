package etg.ipsipdown.launcher.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Проверка Ed25519-подписи манифеста.
 * Публичный ключ зашивается в лаунчер; приватный хранится только у владельца
 * и используется инструментом tools/ManifestSigner для подписи manifest.json.
 * Пока PUBLIC_KEY_B64 пустой — проверка выключена (обратная совместимость).
 */
public final class SignatureUtil {

    /** Base64 X.509-кодированного публичного ключа Ed25519. Получается командой keygen у ManifestSigner. */
    public static final String PUBLIC_KEY_B64 = ""; // TODO: вставь публичный ключ после генерации

    public static boolean isEnabled() {
        return !PUBLIC_KEY_B64.isBlank();
    }

    public static boolean verify(byte[] data, byte[] signature) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_B64)));
        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }

    private SignatureUtil() {
    }
}
