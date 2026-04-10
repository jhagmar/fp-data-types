package persistent_data_structures;

import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class LensTest {

    // Setup Base Lenses (using Lens.of)
    private final Lens<AppState, User> userLens = Lens.of(
            AppState::currentUser,
            (state, newUser) -> new AppState(state.theme(), newUser)
    );
    private final Lens<User, Address> addressLens = Lens.of(
            User::address,
            (user, newAddress) -> new User(user.name(), newAddress)
    );
    private final Lens<Address, String> cityLens = Lens.of(
            Address::city,
            (address, newCity) -> new Address(address.street(), newCity)
    );

    @Test
    void of_withValidArguments_createsLens() {
        Lens<Address, String> validLens = Lens.of(Address::city, (a, c) -> new Address(a.street(), c));
        assertNotNull(validLens);
    }

    @Test
    void of_withNullGetter_throwsException() {
        BiFunction<Address, String, Address> setter = (a, c) -> new Address(a.street(), c);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> Lens.of(null, setter));
        assertEquals("Getter function must not be null", npe.getMessage());
    }

    @Test
    void of_withNullSetter_throwsException() {
        Function<Address, String> getter = Address::city;
        NullPointerException npe = assertThrows(NullPointerException.class, () -> Lens.of(getter, null));
        assertEquals("Setter function must not be null", npe.getMessage());
    }

    @Test
    void get_returnsCorrectPart() {
        Address address = new Address("Avenyn 1", "Gothenburg");
        assertEquals("Gothenburg", cityLens.get(address));
    }

    @Test
    void set_returnsNewWholeWithUpdatedPart() {
        Address original = new Address("Avenyn 1", "Gothenburg");
        Address updated = cityLens.set(original, "Stockholm");

        assertEquals("Stockholm", updated.city());
        assertEquals("Avenyn 1", updated.street()); // Unmodified property remains
        assertNotSame(original, updated);           // Must be a new instance
    }

    @Test
    void modify_appliesFunctionAndReturnsNewWhole() {
        Address original = new Address("Avenyn 1", "Gothenburg");
        Address updated = cityLens.modify(original, city -> city.toUpperCase());

        assertEquals("GOTHENBURG", updated.city());
    }

    // --- 2. Core Operation Tests ---

    @Test
    void modify_withNullModifier_throwsException() {
        Address original = new Address("Avenyn 1", "Gothenburg");
        NullPointerException npe = assertThrows(NullPointerException.class, () -> cityLens.modify(original, null));
        assertEquals("Modifier function must not be null", npe.getMessage());
    }

    @Test
    void compose_createsWorkingDeepLens() {
        AppState initialState = new AppState("Dark", new User("Alice", new Address("Avenyn 1", "Gothenburg")));

        Lens<AppState, String> appToCityLens = userLens.compose(addressLens).compose(cityLens);

        // Test Deep Get
        assertEquals("Gothenburg", appToCityLens.get(initialState));

        // Test Deep Set
        AppState newState = appToCityLens.set(initialState, "Malmö");
        assertEquals("Malmö", newState.currentUser().address().city());
        assertEquals("Dark", newState.theme()); // Ensure root level sibling is untouched
        assertEquals("Alice", newState.currentUser().name()); // Ensure mid level sibling is untouched
    }

    @Test
    void compose_withNullOtherLens_throwsException() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> userLens.compose(null));
        assertEquals("Lens to compose with must not be null", npe.getMessage());
    }

    @Test
    void lensLaw_getPut_leavesWholeUnchanged() {
        // Law: set(whole, get(whole)) == whole
        // Note: For records, .equals() checks value equality, which is what we want to verify
        // that the structural data is identical.
        Address original = new Address("Avenyn 1", "Gothenburg");

        String extractedCity = cityLens.get(original);
        Address rebuilt = cityLens.set(original, extractedCity);

        assertEquals(original, rebuilt, "Get-Put law violated: rebuilding with the extracted part should equal the original");
    }

    // --- 3. Composition Tests ---

    @Test
    void lensLaw_putGet_returnsWhatWasPut() {
        // Law: get(set(whole, part)) == part
        Address original = new Address("Avenyn 1", "Gothenburg");
        String newCity = "Stockholm";

        Address updatedAddress = cityLens.set(original, newCity);
        String extractedAfterPut = cityLens.get(updatedAddress);

        assertEquals(newCity, extractedAfterPut, "Put-Get law violated: getting after setting should return the exact set value");
    }

    // Test Domain Models
    record Address(String street, String city) {
    }

    // --- 4. Lens Laws Validation ---

    record User(String name, Address address) {
    }

    record AppState(String theme, User currentUser) {
    }
}