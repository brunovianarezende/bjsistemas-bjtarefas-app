package nom.bruno.tasksapp.activities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import nom.bruno.tasksapp.BuildConfig;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class MainActivityTest {
    @Test
    public void testIt() {
        // failing test gives much better feedback
        // to show that all works correctly ;)
        assertThat(RuntimeEnvironment.application, notNullValue());
    }
}
