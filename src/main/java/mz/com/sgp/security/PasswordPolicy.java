package mz.com.sgp.security;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || password.isBlank() || password.length() < 12 || password.length() > 128) {
            throw new IllegalArgumentException("A senha deve ter entre 12 e 128 caracteres");
        }
    }
}
