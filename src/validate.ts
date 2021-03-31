const AlphaNumericUnderscore = /^[a-zA-Z0-9_]+$/;

export function objectKeyValuesAreStrings(object: any) {
  if (!isObject(object)) {
    return false;
  }

  const entries = Object.entries(object);

  for (let i = 0; i < entries.length; i++) {
    const [key, value] = entries[i];
    if (!isString(key) || !isString(value)) {
      return false;
    }
  }

  return true;
}

/**
 * Simple is null check.
 *
 * @param value
 * @returns {boolean}
 */
export function isNull(value: any) {
  return value === null;
}

/**
 * Simple is object check.
 *
 * @param value
 * @returns {boolean}
 */
export function isObject(value: any) {
  return value
    ? typeof value === 'object' && !Array.isArray(value) && !isNull(value)
    : false;
}

/**
 * Simple is date check
 * https://stackoverflow.com/a/44198641
 * @param value
 * @returns {boolean}
 */
export function isDate(value: any) {
  // use the global isNaN() and not Number.isNaN() since it will validate an Invalid Date
  return (
    value &&
    Object.prototype.toString.call(value) === '[object Date]' &&
    !isNaN(value)
  );
}

/**
 * Simple is function check
 *
 * @param value
 * @returns {*|boolean}
 */
export function isFunction(value: any) {
  return value ? typeof value === 'function' : false;
}

/**
 * Simple is string check
 * @param value
 * @return {boolean}
 */
export function isString(value: any) {
  return typeof value === 'string';
}

/**
 * Simple is number check
 * @param value
 * @return {boolean}
 */
export function isNumber(value: any) {
  return typeof value === 'number';
}

/**
 * Simple finite check
 * @param value
 * @returns {boolean}
 */
export function isFinite(value: any) {
  return Number.isFinite(value);
}

/**
 * Simple integer check
 * @param value
 * @returns {boolean}
 */
export function isInteger(value: any) {
  return Number.isInteger(value);
}

/**
 * Simple is boolean check
 *
 * @param value
 * @return {boolean}
 */
export function isBoolean(value: any) {
  return typeof value === 'boolean';
}

/**
 *
 * @param value
 * @returns {arg is Array<any>}
 */
export function isArray(value: any) {
  return Array.isArray(value);
}

/**
 *
 * @param value
 * @returns {boolean}
 */
export function isUndefined(value: any) {
  return typeof value === 'undefined';
}

/**
 * /^[a-zA-Z0-9_]+$/
 *
 * @param value
 * @returns {boolean}
 */
export function isAlphaNumericUnderscore(value: any) {
  return AlphaNumericUnderscore.test(value);
}

/**
 * URL test
 * @param url
 * @returns {boolean}
 */
const IS_VALID_URL_REGEX = /^(http|https):\/\/[^ "]+$/;
export function isValidUrl(url: any) {
  return IS_VALID_URL_REGEX.test(url);
}

/**
 * Array includes
 *
 * @param value
 * @param oneOf
 * @returns {boolean}
 */
export function isOneOf(value: any, oneOf = []) {
  if (!isArray(oneOf)) {
    return false;
  }
  // @ts-ignore
  return oneOf.includes(value);
}

export function noop() {
  // noop-🐈
}
