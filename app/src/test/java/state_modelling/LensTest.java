package state_modelling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Lens Functional Specification")
public class LensTest {

    private static class Root {
        final String name;
        final Branch branch;

        Root(String name, Branch branch) {
            this.name = name;
            this.branch = branch;
        }
    }

    private static class Branch {
        final String id;
        final Leaf leaf;

        Branch(String id, Leaf leaf) {
            this.id = id;
            this.leaf = leaf;
        }
    }

    private static class Leaf {
        final String value;

        Leaf(String value) {
            this.value = value;
        }
    }

    private final Lens<Root, Branch> rootToBranch = new Lens<>() {
        @Override
        public Branch get(Root whole) { return whole.branch; }

        @Override
        public Root set(Root whole, Branch part) { return new Root(whole.name, part); }
    };

    private final Lens<Branch, Leaf> branchToLeaf = new Lens<>() {
        @Override
        public Leaf get(Branch whole) { return whole.leaf; }

        @Override
        public Branch set(Branch whole, Leaf part) { return new Branch(whole.id, part); }
    };

    private final Lens<Leaf, String> leafToValue = new Lens<>() {
        @Override
        public String get(Leaf whole) { return whole.value; }

        @Override
        public Leaf set(Leaf whole, String part) { return new Leaf(part); }
    };

    @Nested
    @DisplayName("Basic Operations")
    public class BasicOperations {

        @Test
        @DisplayName("get() should extract the focused part")
        public void testGet() {
            Leaf leaf = new Leaf("data");
            Branch branch = new Branch("b1", leaf);
            Root root = new Root("r1", branch);

            assertEquals(branch, rootToBranch.get(root));
        }

        @Test
        @DisplayName("set() should return a new whole with updated part, preserving immutability")
        public void testSet() {
            Root original = new Root("r1", new Branch("b1", new Leaf("v1")));
            Branch newBranch = new Branch("b2", new Leaf("v2"));

            Root updated = rootToBranch.set(original, newBranch);

            assertNotSame(original, updated, "Should return a new instance");
            assertEquals("r1", updated.name, "Unrelated fields should be preserved");
            assertEquals(newBranch, updated.branch, "Targeted field should be updated");
        }
    }

    @Nested
    @DisplayName("Lens Composition (andThen)")
    public class Composition {

        @Test
        @DisplayName("andThen.get() should drill down through multiple layers")
        public void testCompositionGet() {
            // Compose Root -> Branch -> Leaf -> String
            Lens<Root, String> deepLens = rootToBranch.andThen(branchToLeaf).andThen(leafToValue);

            Root root = new Root("root", new Branch("branch", new Leaf("deep-value")));

            assertEquals("deep-value", deepLens.get(root));
        }

        @Test
        @DisplayName("andThen.set() should update a deep value and reconstruct the hierarchy")
        public void testCompositionSet() {
            Lens<Root, String> deepLens = rootToBranch.andThen(branchToLeaf).andThen(leafToValue);

            Root original = new Root("root", new Branch("branch", new Leaf("old")));
            String newValue = "new";

            Root updated = deepLens.set(original, newValue);

            // Verify the whole tree was reconstructed
            assertEquals(newValue, updated.branch.leaf.value);
            assertEquals("root", updated.name);
            assertEquals("branch", updated.branch.id);
            
            // Verify structural sharing/immutability
            assertNotSame(original, updated);
            assertNotSame(original.branch, updated.branch);
            assertNotSame(original.branch.leaf, updated.branch.leaf);
        }

        @Test
        @DisplayName("andThen should maintain functional identity (Compose A with B, then with C)")
        public void testAssociativity() {
            // (A andThen B) andThen C
            Lens<Root, Leaf> combinedFirst = rootToBranch.andThen(branchToLeaf);
            Lens<Root, String> wayOne = combinedFirst.andThen(leafToValue);

            // A andThen (B andThen C)
            Lens<Branch, String> combinedSecond = branchToLeaf.andThen(leafToValue);
            Lens<Root, String> wayTwo = rootToBranch.andThen(combinedSecond);

            Root root = new Root("r", new Branch("b", new Leaf("v")));

            assertEquals(wayOne.get(root), wayTwo.get(root));
            assertEquals(wayOne.set(root, "new").branch.leaf.value, wayTwo.set(root, "new").branch.leaf.value);
        }
    }
}