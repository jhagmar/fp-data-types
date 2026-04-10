package state_modelling;

import persistent_data_structures.Lens;
import persistent_data_structures.PersistentHashMap;

public sealed interface StateExample permits StateExample.Users, StateExample.Products {

    record State(Users users, Products products) {}

    record Users(PersistentHashMap<UserId, User> userMap) implements StateExample {}

    record Products(PersistentHashMap<ProductId, Product> productMap) implements StateExample {}

    static Lens<State, Users> usersLens() {
        return Lens.of(
            State::users,
            (state, users) -> new State(users, state.products())
        );
    }

    static Lens<State, Products> productsLens() {
        return Lens.of(
            State::products,
            (state, products) -> new State(state.users(), products)
        );
    }
}