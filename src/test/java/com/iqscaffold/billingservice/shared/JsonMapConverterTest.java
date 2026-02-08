package com.iqscaffold.billingservice.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonMapConverterTest {

  private JsonMapConverter converter;

  @BeforeEach
  void setUp() {
    converter = new JsonMapConverter();
  }

  @Test
  @DisplayName("Should convert map to JSON string")
  void shouldConvertMapToJsonString() {
    // Arrange
    var map = Map.<String, Object>of("quota", 10000, "enabled", true, "name", "API Access");

    // Act
    String result = converter.convertToDatabaseColumn(map);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("quota", "enabled", "name");
  }

  @Test
  @DisplayName("Should return null when converting null map")
  void shouldReturnNullWhenConvertingNullMap() {
    // Act
    String result = converter.convertToDatabaseColumn(null);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when converting empty map")
  void shouldReturnNullWhenConvertingEmptyMap() {
    // Arrange
    var emptyMap = Map.<String, Object>of();

    // Act
    String result = converter.convertToDatabaseColumn(emptyMap);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should convert JSON string to map")
  void shouldConvertJsonStringToMap() {
    // Arrange
    String json = "{\"quota\":10000,\"enabled\":true,\"name\":\"API Access\"}";

    // Act
    var result = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result).containsEntry("quota", 10000);
    assertThat(result).containsEntry("enabled", true);
    assertThat(result).containsEntry("name", "API Access");
  }

  @Test
  @DisplayName("Should return null when converting null JSON string")
  void shouldReturnNullWhenConvertingNullJsonString() {
    // Act
    var result = converter.convertToEntityAttribute(null);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when converting empty JSON string")
  void shouldReturnNullWhenConvertingEmptyJsonString() {
    // Act
    var result = converter.convertToEntityAttribute("");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when converting whitespace JSON string")
  void shouldReturnNullWhenConvertingWhitespaceJsonString() {
    // Act
    var result = converter.convertToEntityAttribute("   ");

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should throw exception when converting invalid JSON")
  void shouldThrowExceptionWhenConvertingInvalidJson() {
    // Arrange
    String invalidJson = "not a valid json";

    // Act & Assert
    assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidJson))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Error converting JSON to Map");
  }

  @Test
  @DisplayName("Should handle round-trip conversion")
  void shouldHandleRoundTripConversion() {
    // Arrange
    var originalMap = Map.<String, Object>of(
        "maxUsers", 100,
        "storageGB", 500,
        "supportLevel", "premium"
    );

    // Act
    String json = converter.convertToDatabaseColumn(originalMap);
    var convertedMap = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(convertedMap).containsAllEntriesOf(originalMap);
  }

  @Test
  @DisplayName("Should handle nested objects")
  void shouldHandleNestedObjects() {
    // Arrange
    String json = "{\"config\":{\"timeout\":30,\"retries\":3},\"enabled\":true}";

    // Act
    var result = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).containsKey("config");
    assertThat(result).containsEntry("enabled", true);
  }

  @Test
  @DisplayName("Should handle single entry map")
  void shouldHandleSingleEntryMap() {
    // Arrange
    var singleEntryMap = Map.<String, Object>of("key", "value");

    // Act
    String json = converter.convertToDatabaseColumn(singleEntryMap);
    var result = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result).containsEntry("key", "value");
  }
}
