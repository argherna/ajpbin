package com.github.argherna.ajpbin;

/**
 * Constant values used in this package.
 */
final class Constants {

  /**
   * Private constructor to prevent instantiation.
   */
  private Constants() {
  }

  /** Name of the Output Map attribute processed at the end of the request. */
  static final String OUTPUT_MAP_ATTR_NAME = Constants.class.getPackage().getName() + ".OutputMap";

  /** Content-Type form url encoded. */
  static final String CT_FORM_URLENCODED = "application/x-www-form-urlencoded";

  /** Content-Type application/json. */
  static final String CT_APPLICATION_JSON = "application/json";
}
