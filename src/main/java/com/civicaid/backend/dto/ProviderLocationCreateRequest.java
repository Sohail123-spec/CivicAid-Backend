package com.civicaid.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProviderLocationCreateRequest(

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 250, message = "Address line 1 must be 250 characters or fewer")
        String addressLine1,

        @Size(max = 250, message = "Address line 2 must be 250 characters or fewer")
        String addressLine2,

        @NotBlank(message = "Locality is required")
        @Size(max = 100, message = "Locality must be 100 characters or fewer")
        String locality,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be 100 characters or fewer")
        String city,

        @NotBlank(message = "State code is required")
        @Pattern(
                regexp = "^[A-Z]{2}$",
                message = "State code must be a two-letter uppercase Indian state or UT code"
        )
        String stateCode,

        @Pattern(
                regexp = "^[0-9]{6}$",
                message = "Postal code must contain exactly six digits"
        )
        String postalCode,

        @DecimalMin(
                value = "6.000000",
                message = "Latitude must be at least 6.000000"
        )
        @DecimalMax(
                value = "38.000000",
                message = "Latitude must not exceed 38.000000"
        )
        BigDecimal latitude,

        @DecimalMin(
                value = "68.000000",
                message = "Longitude must be at least 68.000000"
        )
        @DecimalMax(
                value = "98.000000",
                message = "Longitude must not exceed 98.000000"
        )
        BigDecimal longitude,

        @Size(max = 30, message = "Public phone must be 30 characters or fewer")
        String publicPhone,

        @Email(message = "Public email must be a valid email address")
        @Size(max = 254, message = "Public email must be 254 characters or fewer")
        String publicEmail,

        @Size(max = 1000, message = "Accessibility notes must be 1000 characters or fewer")
        String accessibilityNotes,

        @NotBlank(message = "Source name is required")
        @Size(max = 200, message = "Source name must be 200 characters or fewer")
        String sourceName,

        @Pattern(
                regexp = "^https://\\S+$",
                message = "Source URL must use HTTPS"
        )
        @Size(max = 2048, message = "Source URL must be 2048 characters or fewer")
        String sourceUrl

) {

    @AssertTrue(message = "Latitude and longitude must either both be provided or both be omitted")
    public boolean isCoordinatesPaired() {
        return (latitude == null && longitude == null)
                || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "At least one public contact method (phone or email) is required")
    public boolean isPublicContactProvided() {
        return (publicPhone != null && !publicPhone.isBlank())
                || (publicEmail != null && !publicEmail.isBlank());
    }
}
