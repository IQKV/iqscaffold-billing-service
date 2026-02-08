package com.iqscaffold.billingservice.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonListConverterTest {

  private JsonListConverter converter;

  @BeforeEach
  void setUp() {
    converter = new JsonListConverter();
  }

  @Test
  @DisplayName("Should convert list to JSON string")
  void shouldConvertListToJsonString() {
    // Arrange
    var list = List.of("feature1", "feature2", "feature3");

    // Act
    String result = converter.convertToDatabaseColumn(list);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).contains("feature1", "feature2", "feature3");
  }

  @Test
  @DisplayName("Should return null when converting null list")
  void shouldReturnNullWhenConvertingNullList() {
    // Act
    String result = converter.convertToDatabaseColumn(null);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when converting empty list")
  void shouldReturnNullWhenConvertingEmptyList() {
    // Arrange
    var emptyList = List.<String>of();

    // Act
    String result = converter.convertToDatabaseColumn(emptyList);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should convert JSON string to list")
  void shouldConvertJsonStringToList() {
    // Arrange
    String json = "[\"feature1\",\"feature2\",\"feature3\"]";

    // Act
    var result = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result).containsExactly("feature1", "feature2", "feature3");
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
        .hasMessageContaining("Error converting JSON to List");
  }

  @Test
  @DisplayName("Should handle round-trip conversion")
  void shouldHandleRoundTripConversion() {
    // Arrange
    var originalList = List.of("api_access", "advanced_reports", "export_data");

    // Act
    String json = converter.convertToDatabaseColumn(originalList);
    var convertedList = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(convertedList).isEqualTo(originalList);
  }

  @Test
  @DisplayName("Should handle single element list")
  void shouldHandleSingleElementList() {
    // Arrange
    var singleElementList = List.of("single_feature");

    // Act
    String json = converter.convertToDatabaseColumn(singleElementList);
    var result = converter.convertToEntityAttribute(json);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result).containsExactly("single_feature");
  }
}
