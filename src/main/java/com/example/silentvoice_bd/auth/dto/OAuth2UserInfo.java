// src/main/java/com/example/silentvoice_bd/auth/dto/OAuth2UserInfo.java
package com.example.silentvoice_bd.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2UserInfo {

    @NotBlank(message = "Google ID is required")
    private String googleId;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String firstName;
    private String lastName;
    private String profilePicture;

    @NotBlank(message = "ID token is required")
    private String idToken;

    // Helper method to check if user has complete profile
    public boolean hasCompleteProfile() {
        return googleId != null && !googleId.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && fullName != null && !fullName.trim().isEmpty();
    }

    // Helper method to get display name
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        } else if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else {
            return email;
        }
    }
}
