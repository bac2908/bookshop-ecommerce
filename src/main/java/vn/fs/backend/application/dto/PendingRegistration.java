package vn.fs.backend.application.dto;

import java.io.Serializable;

/**
 * Registration data retained only until the email OTP is confirmed.
 * The raw and confirmation passwords must never be stored in the session.
 */
public record PendingRegistration(
        String username,
        String email,
        String name,
        String phone,
        String passwordHash) implements Serializable {
}
