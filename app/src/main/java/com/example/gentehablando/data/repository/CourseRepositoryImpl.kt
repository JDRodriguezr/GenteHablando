package com.example.gentehablando.data.repository

import android.content.Context
import com.example.gentehablando.domain.model.Course
import com.example.gentehablando.domain.model.QuizQuestion
import com.example.gentehablando.domain.repository.CourseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CourseRepository {

    private val prefs = context.getSharedPreferences("app_courses_prefs", Context.MODE_PRIVATE)

    private val courses = listOf(
        Course(
            id = "course_1",
            title = "El poder del silencio activo",
            description = "Aprende a escuchar sin la urgencia de responder.",
            content = "A menudo, cuando alguien nos cuenta un problema, nuestra mente empieza inmediatamente a buscar una solución o un consejo para darle. Esto nace de una buena intención, pero puede invalidar los sentimientos de la otra persona.\n\nEl silencio activo consiste en escuchar poniendo toda tu atención en comprender cómo se siente la otra persona, sin preparar tu respuesta. Cuando haces una pausa después de que el otro termina de hablar, le das espacio para procesar sus propias emociones y, a menudo, ellos mismos encuentran su propia solución.\n\nRecuerda: Estás aquí para acompañar, no para arreglar la vida de nadie.",
            questionsPool = listOf(
                QuizQuestion("¿Qué es el silencio activo?", listOf("Escuchar para comprender sin interrumpir a la persona", "Asentir con la cabeza todo el tiempo mientras el otro habla", "Esperar tu turno para contar una historia tuya parecida", "Darle la razón a la persona en absolutamente todo"), "Escuchar para comprender sin interrumpir a la persona", "La clave está en la intención de tu escucha, que debe enfocarse en la otra persona y no en lo que vas a decir después."),
                QuizQuestion("¿Por qué debemos evitar dar consejos inmediatamente?", listOf("Invalida o minimiza los sentimientos de la otra persona", "Porque a veces los consejos que damos no funcionan", "Porque eso le corresponde a un profesional", "Para no tomar responsabilidad de lo que haga la otra persona"), "Invalida o minimiza los sentimientos de la otra persona", "Saltar a la solución le envía el mensaje al otro de que sus emociones son un problema que debe eliminarse rápido, no algo digno de ser escuchado."),
                QuizQuestion("¿Qué sucede cuando haces una pausa después de que el otro habla?", listOf("Le das espacio para procesar sus propias emociones", "La persona se incomoda y deja de hablar", "El silencio genera aburrimiento en la conversación", "Te da tiempo de pensar en una excusa"), "Le das espacio para procesar sus propias emociones", "El silencio permite que las ideas se asienten. A veces, la persona solo necesita escucharse a sí misma para llegar a una conclusión."),
                QuizQuestion("¿Cuál es nuestro rol principal como oyentes?", listOf("Acompañar y brindar un espacio seguro", "Evitar que la persona tome malas decisiones", "Averiguar quién tiene la culpa de la situación", "Explicarle a la persona qué haría yo en su lugar"), "Acompañar y brindar un espacio seguro", "Recuerda que no eres terapeuta ni un solucionador de problemas, eres un pilar de apoyo en un momento difícil."),
                QuizQuestion("¿Cómo se siente una persona si la interrumpimos para darle la solución?", listOf("Incomprendida, como si su problema fuera minimizado", "Aliviada de ya no tener que pensar más", "Agradecida de que sepamos qué debe hacer", "Inspirada para empezar a solucionar todo"), "Incomprendida, como si su problema fuera minimizado", "La interrupción transmite prisa. Hace sentir que lo que estaban expresando no era lo suficientemente importante para ser escuchado completo."),
                QuizQuestion("¿Qué comunica verdaderamente el silencio activo?", listOf("Empatía y respeto profundo por el espacio del otro", "Que no sabemos absolutamente nada del tema", "Que estamos juzgando lo que acaba de decir", "Que la conversación ya debería terminar"), "Empatía y respeto profundo por el espacio del otro", "El silencio bien usado dice: 'Estoy aquí, no te juzgo y tengo todo el tiempo del mundo para ti'."),
                QuizQuestion("Si la persona hace una pausa larga mientras llora, ¿qué debes hacer?", listOf("Permitir el silencio y estar ahí para ella sin afán", "Preguntar de inmediato: '¿Por qué lloras exactamente?'", "Intentar hacer un chiste para romper el hielo", "Decirle 'No llores' para que se sienta mejor"), "Permitir el silencio y estar ahí para ella sin afán", "Las emociones fuertes necesitan espacio. Empujar a la persona a hablar de inmediato puede hacer que se cierre emocionalmente."),
                QuizQuestion("¿Cuál es un error muy común al escuchar a alguien?", listOf("Estar pensando en lo que vamos a responder nosotros", "Mirar fijamente y asentir todo el tiempo", "Dejar silencios de más de 3 segundos", "Hacer preguntas para que nos aclaren el contexto"), "Estar pensando en lo que vamos a responder nosotros", "A menudo el ego nos gana y queremos sonar inteligentes o útiles, perdiendo el foco en lo que el otro realmente está diciendo."),
                QuizQuestion("¿El silencio activo significa ignorar a la persona?", listOf("No, significa prestar atención plena a cada palabra", "Sí, es dejarla hablar sola para que se canse", "Es poder hacer otra cosa mientras el otro se desahoga", "Es no responder absolutamente nada en todo el chat"), "No, significa prestar atención plena a cada palabra", "Es un acto de participación total. Tu mente está 100% enfocada en asimilar sus palabras y su tono."),
                QuizQuestion("En la mayoría de los casos, ¿quién debe encontrar la solución al problema?", listOf("La persona misma al procesar sus emociones en voz alta", "El oyente, porque ve la situación desde afuera", "Un experto al que la persona deba consultar", "Quien haya causado el problema inicialmente"), "La persona misma al procesar sus emociones en voz alta", "Nadie conoce la vida de alguien mejor que ellos mismos. El trabajo del oyente es solo ser el espejo que les ayude a ver su propia respuesta.")
            )
        ),
        Course(
            id = "course_2",
            title = "Identifica tu emoción",
            description = "Dale un nombre a lo que sientes antes de expresarlo.",
            content = "Cuando nos sentimos abrumados, es fácil decir 'todo está mal' o 'me siento terrible'. Sin embargo, nuestro cerebro procesa mejor las emociones cuando las etiquetamos.\n\nAntes de empezar a escribir en el chat, hazte esta pregunta: ¿Lo que siento en el pecho es tristeza, frustración, miedo o decepción?\n\nAl decirle a tu oyente: 'Siento mucha frustración porque...', estás abriendo una puerta directa a la empatía, y le permites a la otra persona entender tu mundo interno sin tener que adivinar.",
            questionsPool = listOf(
                QuizQuestion("¿Por qué es clave nombrar tu emoción?", listOf("Ayuda al cerebro a procesarla y reduce la intensidad del momento", "Demuestra que tienes un vocabulario emocional extenso", "Obliga a la otra persona a tomar en serio tu problema", "Es el primer paso para poder buscar un culpable"), "Ayuda al cerebro a procesarla y reduce la intensidad del momento", "El simple acto de buscar la palabra exacta para describir tu sentimiento activa la parte lógica de tu cerebro, calmando la parte impulsiva."),
                QuizQuestion("Decir 'Todo está mal' es un ejemplo de...", listOf("Una expresión abrumada sin etiquetar emociones específicas", "Un diagnóstico realista cuando hay una crisis profunda", "La forma más rápida de lograr que alguien nos escuche", "Una técnica avanzada de desahogo emocional"), "Una expresión abrumada sin etiquetar emociones específicas", "Es una generalización que no le da pistas ni a tu cerebro ni al oyente sobre cuál es la verdadera raíz del dolor."),
                QuizQuestion("¿Qué efecto tiene usar la frase 'Yo siento...'?", listOf("Asume la responsabilidad y evita poner a la defensiva al otro", "Hace que el oyente sienta más lástima por nuestra situación", "Obliga al otro a adivinar exactamente qué nos pasó", "Cierra la conversación para evitar que nos den consejos"), "Asume la responsabilidad y evita poner a la defensiva al otro", "Hablar desde el 'Yo' asume la responsabilidad de la emoción, en lugar de poner a la defensiva a quien te escucha."),
                QuizQuestion("¿Qué logra el oyente cuando tú etiquetas tu emoción?", listOf("Entender tu mundo interno sin tener que hacer suposiciones", "Encontrar una solución rápida y eficiente a tu problema", "Saber si tú estás exagerando o tienes la razón", "Identificar quién fue el culpable de la situación"), "Entender tu mundo interno sin tener que hacer suposiciones", "Le quitas el peso de interpretar tus mensajes. Le estás dando el mapa exacto de cómo acercarse a ti."),
                QuizQuestion("Si no sabes qué sientes exactamente, ¿qué es lo más recomendable?", listOf("Tomarte unos minutos en silencio para explorar qué hay en ti", "Cerrar la conversación porque no estás listo para hablar", "Decirle a la otra persona que te ayude a adivinar qué pasa", "Asegurar que estás perfectamente bien para no molestar"), "Tomarte unos minutos en silencio para explorar qué hay en ti", "No hay prisa. Escanear tu propio cuerpo y mente antes de hablar te salvará de proyectar confusión hacia la otra persona."),
                QuizQuestion("¿Qué emoción suele esconderse a menudo detrás de la ira o el enojo?", listOf("Miedo profundo, frustración o un sentimiento de vulnerabilidad", "Simplemente el deseo de querer hacerle daño a los demás", "Felicidad extrema que no sabemos cómo gestionar", "Un exceso de energía que necesita ser gastada gritando"), "Miedo profundo, frustración o un sentimiento de vulnerabilidad", "La ira suele ser una emoción secundaria, un escudo protector que aparece cuando nos sentimos vulnerables o heridos por debajo."),
                QuizQuestion("La frase 'Me siento ignorado cuando haces esto' es un ejemplo de...", listOf("Una comunicación emocional conectada con un hecho concreto", "Un ataque sutil para hacer sentir mal a la otra persona", "Una estrategia para conseguir que nos presten atención", "Una mentira para manipular la empatía del oyente"), "Una comunicación emocional conectada con un hecho concreto", "Conecta una emoción real con un hecho específico, sin insultar ni exagerar."),
                QuizQuestion("¿Por qué los oyentes prefieren que digas qué sientes?", listOf("Porque les quita la presión de tener que interpretar tu dolor", "Para tener argumentos y poder decirte si estás equivocado", "Porque quieren resolver el problema lo antes posible", "Para evitar hablar de sus propios problemas"), "Porque les quita la presión de tener que interpretar tu dolor", "Adivinar genera estrés y malos entendidos. La claridad emocional facilita enormemente el acompañamiento."),
                QuizQuestion("¿Es perjudicial sentir emociones como la rabia o la tristeza profunda?", listOf("No, todas las emociones son respuestas biológicas válidas e importantes", "Sí, debemos esforzarnos siempre por mantenernos positivos", "Solo es perjudicial si duran más de cinco minutos en nosotros", "Sí, porque solo le causan problemas a las personas a nuestro alrededor"), "No, todas las emociones son respuestas biológicas válidas e importantes", "Las emociones son solo mensajeros biológicos. Reprimirlas causa más daño que aceptarlas y transitarlas."),
                QuizQuestion("¿Qué debes hacer antes de hablar en la sala si estás muy alterado?", listOf("Hacer una pausa para preguntarte exactamente qué estás sintiendo", "Enviar muchos mensajes seguidos para que sientan tu urgencia", "Esperar a que se te pase por completo antes de conectarte", "Advertirle al oyente que estás enojado y puedes insultar"), "Hacer una pausa para preguntarte exactamente qué estás sintiendo", "Una pausa de 10 segundos para respirar y nombrar la emoción te transforma de alguien reactivo a alguien reflexivo.")
            )
        ),
        Course(
            id = "course_3",
            title = "Validación vs. Consejos",
            description = "Por qué 'no te preocupes' no ayuda y cómo ser empático.",
            content = "Imagina que alguien te dice: 'Estoy aterrorizado por un examen'. Si respondes: 'No te preocupes, todo va a salir bien', estás minimizando su miedo. Le estás diciendo indirectamente que su miedo es tonto o innecesario.\n\nLa validación emocional es lo opuesto. Es reconocer que los sentimientos del otro tienen sentido en su contexto. Una respuesta validadora sería: 'Es normal que estés asustado, te has esforzado mucho y es algo importante para ti'.\n\nValidar no significa estar de acuerdo con las acciones de la persona, significa reconocer que sus emociones son reales y válidas.",
            questionsPool = listOf(
                QuizQuestion("¿Qué significa validar una emoción?", listOf("Reconocer y aceptar que los sentimientos de la otra persona tienen sentido", "Asegurarle a la persona que tiene absolutamente toda la razón en lo que hizo", "Estar de acuerdo con las acciones que tomó la persona mientras sentía eso", "Intentar solucionar el problema rápidamente para que deje de sentir dolor"), "Reconocer y aceptar que los sentimientos de la otra persona tienen sentido", "Se trata de darle a la persona el derecho de sentirse como se siente, sin intentar corregir su emoción."),
                QuizQuestion("¿Por qué decir 'No te preocupes' suele ser contraproducente?", listOf("Minimiza y le resta importancia a la magnitud de lo que siente el otro", "Porque es una frase cliché que demuestra que no estamos prestando atención", "Porque anima a la persona a preocuparse mucho más por el problema", "Porque demuestra un exceso de empatía que puede resultar asfixiante"), "Minimiza y le resta importancia a la magnitud de lo que siente el otro", "Al decir esto, envías el mensaje de que la persona está exagerando, haciéndola sentir sola en su angustia."),
                QuizQuestion("¿Validar significa estar de acuerdo con las acciones de la persona?", listOf("No, se puede entender la emoción sin estar de acuerdo con la acción", "Sí, debemos darle la razón en todo para que se sienta escuchada", "Sí, implica apoyar incondicionalmente incluso acciones destructivas", "No, significa ignorar a la persona hasta que se calme por completo"), "No, se puede entender la emoción sin estar de acuerdo con la acción", "Puedes entender perfectamente por qué alguien siente mucha rabia, sin estar de acuerdo con que haya roto algo por culpa de esa rabia."),
                QuizQuestion("¿Cuál de estas es una respuesta verdaderamente validadora?", listOf("'Tiene muchísimo sentido que te sientas así tras lo que pasó.'", "'Yo en tu lugar ya habría superado esta situación hace mucho tiempo.'", "'No es para tanto, deberías relajarte y pensar en cosas positivas.'", "'Deberías hacer ejercicio y meditar para que se te pase el malestar.'"), "'Tiene muchísimo sentido que te sientas así tras lo que pasó.'", "Una respuesta empática conecta el evento que le ocurrió a la persona con el por qué es natural que le duela."),
                QuizQuestion("¿Qué efecto emocional suele tener dar un consejo no solicitado?", listOf("Puede hacer sentir a la persona juzgada o totalmente incomprendida", "Siempre resuelve el problema de la persona de forma casi mágica", "Demuestra que somos muy inteligentes y sabemos escuchar activamente", "Acelera el proceso de sanación emocional de la persona que habla"), "Puede hacer sentir a la persona juzgada o totalmente incomprendida", "Cuando alguien solo quiere ser escuchado, un consejo se siente como si quisieras silenciar su dolor para pasar a otra cosa."),
                QuizQuestion("Si alguien llora profundamente en la llamada, ¿cuál es la mejor actitud?", listOf("Acompañarle en el silencio y ofrecerle nuestro apoyo sin juzgarlo", "Decirle repetidamente que deje de llorar porque nos pone tristes a nosotros", "Intentar cambiar de tema rápidamente para distraer a la persona del dolor", "Cortar la llamada por la incomodidad de no saber cómo manejar la situación"), "Acompañarle en el silencio y ofrecerle nuestro apoyo sin juzgarlo", "Reprimir el llanto es represión emocional. Lo más sanador que puedes ofrecer es ser un lugar donde está permitido quebrarse."),
                QuizQuestion("¿Qué es exactamente una 'pregunta abierta' en la escucha activa?", listOf("Aquella que invita a reflexionar, como '¿Cómo te hizo sentir eso?'", "Una pregunta directa que solo puede responderse con 'Sí' o con 'No'", "Una pregunta para desviar la atención, como hablar del clima de hoy", "Cualquier pregunta que nos permita obtener chismes de la situación"), "Aquella que invita a reflexionar, como '¿Cómo te hizo sentir eso?'", "Las preguntas abiertas no pueden responderse con monosílabos. Obligan a la persona a mirar hacia adentro y desarrollar su historia."),
                QuizQuestion("¿Por qué es muy poco útil utilizar la frase 'Yo en tu lugar haría...'?", listOf("Asume incorrectamente que tu contexto y herramientas son iguales a los suyos", "Porque a la persona no le importa en absoluto tu vida ni tu experiencia", "Porque hablar de uno mismo es de muy mala educación en cualquier contexto", "Porque toma mucho tiempo explicar la historia y la persona se aburrirá"), "Asume incorrectamente que tu contexto y herramientas son iguales a los suyos", "Cada persona tiene una tolerancia al dolor, recursos y miedos distintos. Lo que es fácil para ti puede ser imposible para el otro en este momento."),
                QuizQuestion("¿Qué es lo que verdaderamente logra la empatía en una conversación?", listOf("Crea un puente seguro de conexión humana profunda entre ambos", "Generar un sentimiento de lástima por la situación de la otra persona", "Permite que la persona se aísle emocionalmente para sanar sola", "Resolver problemas técnicos o matemáticos que la persona no lograba"), "Crea un puente seguro de conexión humana profunda entre ambos", "La empatía es bajar al mismo pozo oscuro donde está la otra persona y decirle: 'No estás solo aquí'. No es sacarla del pozo a la fuerza."),
                QuizQuestion("¿Cuál de las siguientes frases EVITA por completo la validación emocional?", listOf("'A muchas otras personas en el mundo les va mucho peor que a ti.'", "'Es totalmente comprensible que te sientas tan enojado por eso.'", "'Realmente me duele verte pasar por esta situación tan difícil.'", "'No sé qué decirte ahora, pero aquí estoy para escucharte.'"), "'A muchas otras personas en el mundo les va mucho peor que a ti.'", "Comparar el dolor de una persona con el de otros es una de las formas más tóxicas de invalidación. El dolor es relativo y personal.")
            )
        )
    )

    override fun getCourses(): List<Course> = courses

    override fun getCourseById(courseId: String): Course? = courses.find { it.id == courseId }

    override fun observeCompletedCourses(): Flow<Map<String, Int>> = callbackFlow {
        // Leemos primero el caché local. Estructura: "score_courseId" -> Int (puntaje)
        val currentScores = mutableMapOf<String, Int>()
        courses.forEach { course ->
            val score = prefs.getInt("score_${course.id}", -1)
            if (score != -1) currentScores[course.id] = score
        }
        trySend(currentScores)

        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            val listener = firestore.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val remoteScores = snapshot.get("courseScores") as? Map<String, Long> ?: emptyMap()

                        var changed = false
                        remoteScores.forEach { (cId, rScore) ->
                            val currentLocalScore = currentScores[cId] ?: -1
                            if (rScore.toInt() > currentLocalScore) {
                                currentScores[cId] = rScore.toInt()
                                prefs.edit().putInt("score_$cId", rScore.toInt()).apply()
                                changed = true
                            }
                        }

                        if (changed) trySend(currentScores)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            awaitClose { }
        }
    }

    override suspend fun markCourseCompleted(courseId: String, score: Int) {
        val currentScore = prefs.getInt("score_$courseId", -1)
        if (score > currentScore) {
            prefs.edit().putInt("score_$courseId", score).apply()
        } else return // Si saco peor puntaje que antes, no lo sobreescribimos

        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            try {
                firestore.collection("users").document(user.uid)
                    .set(mapOf("courseScores" to mapOf(courseId to score)), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun syncLocalProgressToRemote() {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            val currentScores = mutableMapOf<String, Int>()
            courses.forEach { course ->
                val score = prefs.getInt("score_${course.id}", -1)
                if (score != -1) currentScores[course.id] = score
            }
            if (currentScores.isNotEmpty()) {
                try {
                    firestore.collection("users").document(user.uid)
                        .set(mapOf("courseScores" to currentScores), SetOptions.merge())
                        .await()
                } catch (_: Exception) {}
            }
        }
    }

    override fun getQuizCooldown(courseId: String): Long {
        return prefs.getLong("cooldown_$courseId", 0L)
    }

    override fun setQuizCooldown(courseId: String, cooldownEndTimeMs: Long) {
        prefs.edit().putLong("cooldown_$courseId", cooldownEndTimeMs).apply()
    }
}
