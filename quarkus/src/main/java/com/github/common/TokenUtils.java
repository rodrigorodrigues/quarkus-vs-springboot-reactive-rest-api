package com.github.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenUtils {
    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);
	private static TokenUtils instance;
	private String privateKeyPath;
	private TokenUtils() {}

	/**
	 * Utility method to generate a JWT string from a JSON resource file that is signed by the privateKey.pem
	 * test resource key, possibly with invalid fields.
	 *
	 * @param authorizationDto - dto request
	 * @return the JWT string
	 * @throws Exception on parse failure
	 */
	public String generateTokenString(AuthorizationDto authorizationDto)
			throws Exception {
		// Use the test private key associated with the test public key for a valid signature
		PrivateKey pk = readPrivateKey();
		return generateTokenString(pk, "test", authorizationDto);
	}

	public String generateTokenString(PrivateKey privateKey, String kid,
			AuthorizationDto authorizationDto) {

		JwtClaimsBuilder claims = Jwt.claims();
		long currentTimeInSecs = currentTimeInSecs();
		long exp = currentTimeInSecs + Duration.ofHours(1).getSeconds();

		claims.claim("jti", UUID.randomUUID());
		claims.claim("authorities", Arrays.asList(authorizationDto.getRoles()));
		claims.issuedAt(currentTimeInSecs);
		claims.expiresAt(exp);
		claims.claim("scope", Collections.singletonList("read"));
		claims.subject(authorizationDto.getUser());
		claims.issuer("jwt");
		return claims.jws().signatureKeyId(kid).sign(privateKey);
	}

	/**
	 * Read a PEM encoded private key from the classpath
	 *
	 * @return PrivateKey
	 * @throws Exception on decode failure
	 */
	public PrivateKey readPrivateKey() throws Exception {
		try (InputStream contentIS = new FileInputStream(getPrivateKeyFile(privateKeyPath))) {
			byte[] tmp = new byte[4096];
			int length = contentIS.read(tmp);
			return decodePrivateKey(new String(tmp, 0, length, "UTF-8"));
		}
	}

	/**
	 * Decode a PEM encoded private key string to an RSA PrivateKey
	 *
	 * @param pemEncoded - PEM string for private key
	 * @return PrivateKey
	 * @throws Exception on decode failure
	 */
	public PrivateKey decodePrivateKey(final String pemEncoded) throws Exception {
		byte[] encodedBytes = toEncodedBytes(pemEncoded);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(keySpec);
	}

	private byte[] toEncodedBytes(final String pemEncoded) {
		return Base64.getDecoder().decode(removeBeginEnd(pemEncoded));
	}

	private String removeBeginEnd(String pem) {
		pem = pem.replaceAll("-----BEGIN (.*)-----", "");
		pem = pem.replaceAll("-----END (.*)----", "");
		pem = pem.replaceAll("\r\n", "");
		pem = pem.replaceAll("\n", "");
		return pem.trim();
	}

	/**
	 * @return the current time in seconds since epoch
	 */
	public int currentTimeInSecs() {
		long currentTimeMS = System.currentTimeMillis();
		return (int) (currentTimeMS / 1000);
	}

	public static TokenUtils getInstance(String privateKeyPath, String publicKeyPath) {
		if (instance == null) {
			try {
                File privateKey = getPrivateKeyFile(privateKeyPath);
                if (!privateKey.exists()) {
					KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
					kpg.initialize(2048);
					KeyPair kp = kpg.generateKeyPair();
					Key pub = kp.getPublic();
					Key pvt = kp.getPrivate();

					Base64.Encoder encoder = Base64.getEncoder();

					Files.write(privateKey.toPath(),
							Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
									.encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));

                    File publicKey = (StringUtils.isNotBlank(publicKeyPath) ? new File(publicKeyPath) : new File(System.getProperty("java.io.tmpdir"), "publicKey.pem"));

					Files.write(publicKey.toPath(),
							Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
									.encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));
				}
				instance = new TokenUtils();
				instance.privateKeyPath = privateKeyPath;
			} catch (Exception e) {
				log.error("Error generate cert", e);
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

    private static File getPrivateKeyFile(String privateKeyPath) {
        return StringUtils.isNotBlank(privateKeyPath) ? new File(privateKeyPath) : new File(System.getProperty("java.io.tmpdir"), "privateKey.pem");
    }
}
