package etg.ipsipdown.launcher.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Инструмент владельца сборки: генерация ключей и подпись manifest.json.
 * Запуск (из папки с jar лаунчера):
 *
 *   1. Сгенерировать ключи (один раз):
 *      java -cp EternalSky-2.0.0-jar-with-dependencies.jar etg.ipsipdown.launcher.tools.ManifestSigner keygen
 *      -> создаст manifest-signing.key (ПРИВАТНЫЙ — никому не показывать, в git не класть!)
 *      -> выведет публичный ключ: его вставить в SignatureUtil.PUBLIC_KEY_B64
 *
 *   2. Подписать манифест (при каждом обновлении сборки):
 *      java -cp ... etg.ipsipdown.launcher.tools.ManifestSigner sign manifest-signing.key manifest.json
 *      -> создаст manifest.json.sig — загрузить на R2 рядом с manifest.json
 */
public class ManifestSigner {

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && args[0].equals("keygen")) {
            keygen();
        } else if (args.length >= 3 && args[0].equals("sign")) {
            sign(Path.of(args[1]), Path.of(args[2]));
        } else {
            System.out.println("Использование:");
            System.out.println("  keygen                          — создать пару ключей");
            System.out.println("  sign <ключ.key> <manifest.json> — подписать манифест");
        }
    }

    private static void keygen() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = kpg.generateKeyPair();

        Path keyFile = Path.of("manifest-signing.key");
        Files.writeString(keyFile, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));

        System.out.println("Приватный ключ сохранён: " + keyFile.toAbsolutePath());
        System.out.println("НИКОМУ его не передавай и не клади в git!");
        System.out.println();
        System.out.println("Публичный ключ (вставь в SignatureUtil.PUBLIC_KEY_B64):");
        System.out.println(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
    }

    private static void sign(Path keyFile, Path manifestFile) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(keyFile).trim());
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        byte[] manifest = Files.readAllBytes(manifestFile);
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(privateKey);
        sig.update(manifest);

        Path sigFile = Path.of(manifestFile + ".sig");
        Files.writeString(sigFile, Base64.getEncoder().encodeToString(sig.sign()));
        System.out.println("Подпись сохранена: " + sigFile.toAbsolutePath());
        System.out.println("Загрузи её на R2 рядом с manifest.json");
    }
}
