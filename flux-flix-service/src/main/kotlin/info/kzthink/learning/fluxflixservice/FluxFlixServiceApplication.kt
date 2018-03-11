package info.kzthink.learning.fluxflixservice

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.util.*

@SpringBootApplication
class FluxFlixServiceApplication {

    @Bean
    fun runner (mr:MovieRepository) = ApplicationRunner{
        val movies = Flux.just("Silence of the Lambdas", "AEon Flux", "Back to the Future")
                .flatMap { mr.save(Movie(title = it)) }

        //begin process
        mr
                .deleteAll()
                .thenMany(movies)
                .thenMany(mr.findAll())
                .subscribe( {println(it)} )

    }
}

@RestController
class MovieController(var ms: MovieService){

    @GetMapping("/movies", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun all () = ms.all()

    @GetMapping("/movies/old")
    fun allOld () = ms.allOld()

    @GetMapping("/movies/{id}")
    fun byId (@PathVariable id :String) = ms.byId(id)

    @GetMapping("/movies/{id}/events", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun events (@PathVariable id: String) = ms.events(id)

    @GetMapping("/movies/{id}/events/old")
    fun eventsOld (@PathVariable id: String) = ms.events(id)
}

@Service
class MovieService (private val mr:MovieRepository){

    fun all() = mr.findAll().delayElements(Duration.ofSeconds(2L))

    fun allOld() = mr.findAll().delayElements(Duration.ofSeconds(2L))

    fun byId(id: String) = mr.findById(id)

    fun events(id: String) = Flux
            .generate({ sink: SynchronousSink<MovieEvent> -> sink.next(MovieEvent(id, Date())) })
            .delayElements(Duration.ofSeconds(1L))
            .windowTimeout(10, Duration.ofMillis(1))
}

data class MovieEvent(var movieId: String? = null, var date: Date? = null)

interface MovieRepository : ReactiveMongoRepository<Movie, String>

@Document
data class Movie(@Id var id: String? = null, var title: String? = null)




fun main(args: Array<String>) {
    runApplication<FluxFlixServiceApplication>(*args)
}