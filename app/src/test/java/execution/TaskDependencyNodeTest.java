package execution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TaskDependencyNode Specification")
public class TaskDependencyNodeTest {

    private TestPayload payload;
    private StubListener listener;
    private TaskDependencyNode<TestPayload> node;

    @BeforeEach
    public void setUp() {
        payload = new TestPayload("root-task");
        listener = new StubListener();
        node = TaskDependencyNode.create(payload, listener);
    }

    public static class TestPayload implements ConflictAware<TestPayload>, Traceable {
        private final String id;
        public boolean conflictDetected = false;

        public TestPayload(String id) { this.id = id; }

        @Override public String getTraceId() { return id; }

        @Override
        public boolean conflictsWith(TestPayload other) {
            this.conflictDetected = true;
            return false;
        }
    }

    public static class StubListener implements TaskResolutionListener<TestPayload> {
        public List<TaskDependencyNode<TestPayload>> resolvedNodes = new ArrayList<>();

        @Override
        public void onTaskResolved(TaskDependencyNode<TestPayload> node) {
            resolvedNodes.add(node);
        }
    }

    @Nested
    @DisplayName("Creation and Initialization")
    public class Construction {

        @Test
        @DisplayName("Should initialize with default state")
        public void shouldConstructCorrectly() {
            assertEquals(0, node.getInDegree());
            assertEquals(payload, node.getTask().getPayload());
            assertEquals("root-task", node.getTraceId());
        }

        @Test
        @DisplayName("Should fail on null inputs")
        public void shouldThrowOnNulls() {
            assertThrows(NullPointerException.class, () -> TaskDependencyNode.create(null, listener));
            assertThrows(NullPointerException.class, () -> TaskDependencyNode.create(payload, null));
        }
    }

    @Nested
    @DisplayName("Dependency Management")
    public class Dependencies {

        @Test
        @DisplayName("Adding dependent should link nodes and increment in-degree")
        public void shouldHandleDependencyLinking() {
            TaskDependencyNode<TestPayload> dependent = TaskDependencyNode.create(new TestPayload("dep"), listener);
            
            node.addDependent(dependent);

            assertEquals(1, dependent.getInDegree());
            assertTrue(node.getDependents().contains(dependent));
        }

        @Test
        @DisplayName("Adding same dependent twice should be idempotent")
        public void shouldBeIdempotent() {
            TaskDependencyNode<TestPayload> dependent = TaskDependencyNode.create(new TestPayload("dep"), listener);
            
            node.addDependent(dependent);
            node.addDependent(dependent);

            assertEquals(1, dependent.getInDegree(), "In-degree should only increment once");
            assertEquals(1, node.getDependents().size());
        }

        @Test
        @DisplayName("Should throw exception for circular self-dependency")
        public void shouldPreventSelfDependency() {
            assertThrows(IllegalArgumentException.class, () -> node.addDependent(node));
        }
        
        @Test
        @DisplayName("Should throw if dependent node is null")
        public void shouldThrowIfDependentIsNull() {
            assertThrows(NullPointerException.class, () -> node.addDependent(null));
        }
    }

    @Nested
    @DisplayName("Resolution Logic")
    public class Resolution {

        @Test
        @DisplayName("Resolving should update dependents and clear graph connections")
        public void shouldResolveDependents() {
            TaskDependencyNode<TestPayload> dep = TaskDependencyNode.create(new TestPayload("dep"), listener);
            node.addDependent(dep);

            node.resolve();

            assertEquals(0, dep.getInDegree(), "Dependent should have its in-degree decremented");
            assertTrue(node.getDependents().isEmpty(), "Node should clear its dependents list after resolution");
        }

        @Test
        @DisplayName("TaskHandle should trigger the resolution listener")
        public void taskHandleShouldTriggerListener() {
            Task<TestPayload> handle = node.getTask();
            
            handle.resolve();

            assertEquals(1, listener.resolvedNodes.size());
            assertEquals(node, listener.resolvedNodes.get(0));
        }
    }

    @Nested
    @DisplayName("Delegation and Representation")
    public class Delegation {

        @Test
        @DisplayName("Should delegate conflict check to internal payload")
        public void shouldDelegateConflict() {
            TaskDependencyNode<TestPayload> otherNode = TaskDependencyNode.create(new TestPayload("other"), listener);
            
            node.conflictsWith(otherNode);
            
            assertTrue(payload.conflictDetected, "Payload conflict logic should have been invoked");
        }

        @Test
        @DisplayName("ToString should contain state information")
        public void testToString() {
            String result = node.toString();
            assertTrue(result.contains("root-task"));
            assertTrue(result.contains("inDegree=0"));
        }
        
        @Test
        @DisplayName("TaskHandle should delegate TraceId")
        public void taskHandleTraceId() {
            assertEquals("root-task", node.getTask().getTraceId());
        }
    }
}