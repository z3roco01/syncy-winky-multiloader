/**
 * Precompiled [neoforge-mutex.gradle.kts][Neoforge_mutex_gradle] script plugin.
 *
 * @see Neoforge_mutex_gradle
 */
public
class NeoforgeMutexPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Neoforge_mutex_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
