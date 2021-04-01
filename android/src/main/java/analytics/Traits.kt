package analytics

import analytics.internal.Private
import analytics.internal.Utils
import analytics.internal.Utils.NullableConcurrentHashMap
import android.content.Context
import java.text.ParseException
import java.util.*

/**
 * A class representing information about a user.
 *
 *
 * Traits can be anything you want, but some of them have semantic meaning and we treat them in
 * special ways. For example, whenever we see an email trait, we expect it to be the user's email
 * address. And we'll send this on to integrations that need an email, like Mailchimp. For that
 * reason, you should only use special traits for their intended purpose.
 *
 *
 * Traits are persisted to disk, and will be remembered between application and system reboots.
 */
class Traits : ValueMap {
  /** For deserialization from disk by [Cache].  */
  @Private
  internal constructor(delegate: Map<String, Any>?) : super(delegate) {
  }

  // Public Constructor
  constructor() {}
  constructor(initialCapacity: Int) : super(initialCapacity) {}

  fun unmodifiableCopy(): Traits {
    val map = LinkedHashMap(this)
    return Traits(Collections.unmodifiableMap(map))
  }

  /**
   * Private API, users should call [analytics.Analytics.identify]
   * instead. Note that this is unable to enforce it, users can easily do traits.put(id, ..);
   */
  fun putUserId(id: String): Traits {
    return putValue(USER_ID_KEY, id)
  }

  fun userId(): String {
    return getString(USER_ID_KEY)
  }

  fun putAnonymousId(id: String): Traits {
    return putValue(ANONYMOUS_ID_KEY, id)
  }

  fun anonymousId(): String {
    return getString(ANONYMOUS_ID_KEY)
  }

  /**
   * Returns the currentId the user is identified with. This could be the user id or the anonymous
   * ID.
   */
  fun currentId(): String {
    val userId = userId()
    return if (Utils.isNullOrEmpty(userId)) anonymousId() else userId
  }

  /** Set an address for the user or group.  */
  fun putAddress(address: Address): Traits {
    return putValue(ADDRESS_KEY, address)
  }

  fun address(): Address {
    return getValueMap(ADDRESS_KEY, Address::class.java)
  }

  /** Set the age of a user.  */
  fun putAge(age: Int): Traits {
    return putValue(AGE_KEY, age)
  }

  fun age(): Int {
    return getInt(AGE_KEY, 0)
  }

  /** Set a URL to an avatar image for the user or group.  */
  fun putAvatar(avatar: String): Traits {
    return putValue(AVATAR_KEY, avatar)
  }

  fun avatar(): String {
    return getString(AVATAR_KEY)
  }

  /** Set the user's birthday.  */
  fun putBirthday(birthday: Date?): Traits {
    return putValue(BIRTHDAY_KEY, Utils.toISO8601Date(birthday))
  }

  fun birthday(): Date? {
    return try {
      val birthday = getString(BIRTHDAY_KEY)
      if (Utils.isNullOrEmpty(birthday)) null else Utils.toISO8601Date(birthday)
    } catch (e: ParseException) {
      null
    }
  }

  /**
   * Set the date the user's or group's account was first created. We accept date objects and a
   * wide range of date formats, including ISO strings and Unix timestamps. Feel free to use
   * whatever format is easiest for you - although ISO string is recommended for Android.
   */
  fun putCreatedAt(createdAt: String): Traits {
    return putValue(CREATED_AT_KEY, createdAt)
  }

  fun createdAt(): String {
    return getString(CREATED_AT_KEY)
  }

  /** Set a description of the user or group, like a personal bio.  */
  fun putDescription(description: String): Traits {
    return putValue(DESCRIPTION_KEY, description)
  }

  fun description(): String {
    return getString(DESCRIPTION_KEY)
  }

  /** Set the email address of a user or group.  */
  fun putEmail(email: String): Traits {
    return putValue(EMAIL_KEY, email)
  }

  fun email(): String {
    return getString(EMAIL_KEY)
  }

  /** Set the number of employees of a group, typically used for companies.  */
  fun putEmployees(employees: Long): Traits {
    return putValue(EMPLOYEES_KEY, employees)
  }

  fun employees(): Long {
    return getLong(EMPLOYEES_KEY, 0)
  }

  /** Set the fax number of a user or group.  */
  fun putFax(fax: String): Traits {
    return putValue(FAX_KEY, fax)
  }

  fun fax(): String {
    return getString(
      FAX_KEY) // todo: maybe remove this, I doubt any bundled integration uses fax
  }

  /** Set the first name of a user.  */
  fun putFirstName(firstName: String): Traits {
    return putValue(FIRST_NAME_KEY, firstName)
  }

  fun firstName(): String {
    return getString(FIRST_NAME_KEY)
  }

  /** Set the gender of a user.  */
  fun putGender(gender: String): Traits {
    return putValue(GENDER_KEY, gender)
  }

  fun gender(): String {
    return getString(GENDER_KEY)
  }

  /** Set the industry the user works in, or a group is part of.  */
  fun putIndustry(industry: String): Traits {
    return putValue(INDUSTRY_KEY, industry)
  }

  fun industry(): String {
    return getString(INDUSTRY_KEY)
  }

  /** Set the last name of a user.  */
  fun putLastName(lastName: String): Traits {
    return putValue(LAST_NAME_KEY, lastName)
  }

  fun lastName(): String {
    return getString(LAST_NAME_KEY)
  }

  /** Set the name of a user or group.  */
  fun putName(name: String): Traits {
    return putValue(NAME_KEY, name)
  }

  fun name(): String? {
    val name = getString(NAME_KEY)
    if (Utils.isNullOrEmpty(name) && Utils.isNullOrEmpty(firstName()) && Utils.isNullOrEmpty(lastName())) {
      return null
    }
    return if (Utils.isNullOrEmpty(name)) {
      val stringBuilder = StringBuilder()
      val firstName = firstName()
      var appendSpace = false
      if (!Utils.isNullOrEmpty(firstName)) {
        appendSpace = true
        stringBuilder.append(firstName)
      }
      val lastName = lastName()
      if (!Utils.isNullOrEmpty(lastName)) {
        if (appendSpace) stringBuilder.append(' ')
        stringBuilder.append(lastName)
      }
      stringBuilder.toString()
    } else {
      name
    }
  }

  /** Set the phone number of a user or group.  */
  fun putPhone(phone: String): Traits {
    return putValue(PHONE_KEY, phone)
  }

  fun phone(): String {
    return getString(PHONE_KEY)
  }

  /**
   * Set the title of a user, usually related to their position at a specific company, for example
   * "VP of Engineering"
   */
  fun putTitle(title: String): Traits {
    return putValue(TITLE_KEY, title)
  }

  fun title(): String {
    return getString(TITLE_KEY)
  }

  /**
   * Set the user's username. This should be unique to each user, like the usernames of Twitter or
   * GitHub.
   */
  fun putUsername(username: String): Traits {
    return putValue(USERNAME_KEY, username)
  }

  fun username(): String {
    return getString(USERNAME_KEY)
  }

  /** Set the website of a user or group.  */
  fun putWebsite(website: String): Traits {
    return putValue(WEBSITE_KEY, website)
  }

  fun website(): String {
    return getString(WEBSITE_KEY)
  }

  override fun putValue(key: String, value: Any): Traits {
    super.putValue(key, value)
    return this
  }

  /** Represents information about the address of a user or group.  */
  class Address : ValueMap {
    // Public constructor
    constructor() {}

    // For deserialization
    constructor(map: Map<String?, Any?>?) : super(map) {}

    override fun putValue(key: String, value: Any): Address {
      super.putValue(key, value)
      return this
    }

    fun putCity(city: String): Address {
      return putValue(ADDRESS_CITY_KEY, city)
    }

    fun city(): String {
      return getString(ADDRESS_CITY_KEY)
    }

    fun putCountry(country: String): Address {
      return putValue(ADDRESS_COUNTRY_KEY, country)
    }

    fun country(): String {
      return getString(ADDRESS_COUNTRY_KEY)
    }

    fun putPostalCode(postalCode: String): Address {
      return putValue(ADDRESS_POSTAL_CODE_KEY, postalCode)
    }

    fun postalCode(): String {
      return getString(ADDRESS_POSTAL_CODE_KEY)
    }

    fun putState(state: String): Address {
      return putValue(ADDRESS_STATE_KEY, state)
    }

    fun state(): String {
      return getString(ADDRESS_STATE_KEY)
    }

    fun putStreet(street: String): Address {
      return putValue(ADDRESS_STREET_KEY, street)
    }

    fun street(): String {
      return getString(ADDRESS_STREET_KEY)
    }

    companion object {
      private const val ADDRESS_CITY_KEY = "city"
      private const val ADDRESS_COUNTRY_KEY = "country"
      private const val ADDRESS_POSTAL_CODE_KEY = "postalCode"
      private const val ADDRESS_STATE_KEY = "state"
      private const val ADDRESS_STREET_KEY = "street"
    }
  }

  internal class Cache(context: Context?, cartographer: Cartographer?, tag: String) : ValueMap.Cache<Traits>(context, cartographer, TRAITS_CACHE_PREFIX + tag, tag, Traits::class.java) {
    public override fun create(map: Map<String, Any>): Traits {
      // Analytics client can be called on any thread, so this instance should be thread safe.
      return Traits(NullableConcurrentHashMap(map))
    }

    companion object {
      // todo: remove. This is legacy behaviour from before we started namespacing the entire
      // shared
      // preferences object and were namespacing keys instead.
      private const val TRAITS_CACHE_PREFIX = "traits-"
    }
  }

  companion object {
    private const val AVATAR_KEY = "avatar"
    private const val CREATED_AT_KEY = "createdAt"
    private const val DESCRIPTION_KEY = "description"
    private const val EMAIL_KEY = "email"
    private const val FAX_KEY = "fax"
    private const val ANONYMOUS_ID_KEY = "anonymousId"
    private const val USER_ID_KEY = "userId"
    private const val NAME_KEY = "name"
    private const val PHONE_KEY = "phone"
    private const val WEBSITE_KEY = "website"

    // For Identify Calls
    private const val AGE_KEY = "age"
    private const val BIRTHDAY_KEY = "birthday"
    private const val FIRST_NAME_KEY = "firstName"
    private const val GENDER_KEY = "gender"
    private const val LAST_NAME_KEY = "lastName"
    private const val TITLE_KEY = "title"
    private const val USERNAME_KEY = "username"

    // For Group calls
    private const val EMPLOYEES_KEY = "employees"
    private const val INDUSTRY_KEY = "industry"

    // Address
    private const val ADDRESS_KEY = "address"

    /**
     * Create a new Traits instance with an anonymous ID. Analytics client can be called on any
     * thread, so this instance is thread safe.
     */
    @JvmStatic
    fun create(): Traits {
      val traits = Traits(NullableConcurrentHashMap())
      traits.putAnonymousId(UUID.randomUUID().toString())
      return traits
    }
  }
}
