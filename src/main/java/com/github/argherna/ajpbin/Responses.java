package com.github.argherna.ajpbin;

/**
 * Utilities for setting and processing response information.
 */
final class Responses {

  /**
   * Private constructor to prevent instantiation.
   */
  private Responses() {
  }

  /**
   * Return {@code true} if the status code is in the informational range, {@code false} otherwise.
   * 
   * <p>
   * Status codes whose values fall between {@code 100} and {@code 199} inclusive are in the
   * informational range.
   * 
   * @param sc status code.
   * @return {@code true} if the status code is in the informational range.
   */
  static boolean isStatusInformation(int sc) {
    return (sc >= 100) && (sc <= 199);
  }

  /**
   * Return {@code true} if the status code is in the success range, {@code false} otherwise.
   * 
   * <p>
   * Status codes whose values fall between {@code 200} and {@code 299} inclusive are in the success
   * range.
   * 
   * @param sc status code.
   * @return {@code true} if the status code is in the success range.
   */
  static boolean isStatusSuccess(int sc) {
    return (sc >= 200) && (sc <= 299);
  }

  /**
   * Return {@code true} if the status code is in the redirect range, {@code false} otherwise.
   * 
   * <p>
   * Status codes whose values fall between {@code 300} and {@code 399} inclusive are in the
   * redirect range.
   * 
   * @param sc status code.
   * @return {@code true} if the status code is in the redirect range.
   */
  static boolean isStatusRedirection(int sc) {
    return (sc >= 300) && (sc <= 399);
  }

  /**
   * Return {@code true} if the status code is in the client error range, {@code false} otherwise.
   * 
   * <p>
   * Status codes whose values fall between {@code 400} and {@code 499} inclusive are in the client
   * error range.
   * 
   * @param sc status code.
   * @return {@code true} if the status code is in the client error range.
   */
  static boolean isStatusClientError(int sc) {
    return (sc >= 400) && (sc <= 499);
  }

  /**
   * Return {@code true} if the status code is in the server error range, {@code false} otherwise.
   * 
   * <p>
   * Status codes whose values fall between {@code 500} and {@code 599} inclusive are in the server
   * error range.
   * 
   * @param sc status code.
   * @return {@code true} if the status code is in the server error range.
   */
  static boolean isStatusServerError(int sc) {
    return (sc >= 500) && (sc <= 599);
  }

  /**
   * Return {@code true} if the status code is an error, {@code false} otherwise.
   * 
   * <p>
   * An error is considered a status code whose value is in the client error or server error range.
   * 
   * @param sc status code.
   * @return {@code true} if the status code is in the client error or server error range.
   */
  static boolean isError(int sc) {
    return isStatusClientError(sc) || isStatusServerError(sc);
  }
}
