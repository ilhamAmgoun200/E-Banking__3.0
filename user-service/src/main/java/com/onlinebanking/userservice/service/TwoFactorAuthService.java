// TwoFactorAuthService.java

package com.onlinebanking.userservice.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorAuthService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();

    // We create verifier per verification call (very cheap) → we can customize it
    public boolean verifyCode(String secret, String code) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        verifier.setTimePeriod(30);                // ← correct method name
        verifier.setAllowedTimePeriodDiscrepancy(1); // ← correct method name

        return verifier.isValidCode(secret, code.trim());
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String generateQrUrl(String username, String secret) {
        return "otpauth://totp/SecureBank:" + username +
                "?secret=" + secret +
                "&issuer=SecureBank";
    }
}