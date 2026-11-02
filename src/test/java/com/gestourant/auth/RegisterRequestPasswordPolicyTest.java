package com.gestourant.auth;

import com.gestourant.user.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestPasswordPolicyTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsPasswordMeetingTheRegistrationPolicy() {
        assertTrue(validator.validate(request("Password1!abc")).isEmpty());
        assertTrue(validator.validate(request("Password1!ab")).isEmpty());
        assertTrue(validator.validate(request("Password1!abc" + "x".repeat(59))).isEmpty());
    }

    @Test
    void rejectsPasswordMissingAnyRequiredCharacterClassOrLength() {
        assertFalse(validator.validate(request("Password1!")).isEmpty());
        assertFalse(validator.validate(request("password1!abc")).isEmpty());
        assertFalse(validator.validate(request("PASSWORD1!ABC")).isEmpty());
        assertFalse(validator.validate(request("Password!abcd")).isEmpty());
        assertFalse(validator.validate(request("Password12345")).isEmpty());
        assertFalse(validator.validate(request("Password1!abc" + "a".repeat(60))).isEmpty());
    }

    private RegisterRequest request(String password) {
        return new RegisterRequest("maria.user", "maria@example.com", password, Role.EMPLEADO,
            null, null, null, true);
    }
}
