package id.supriadi.quarkus;

import id.supriadi.quarkus.config.ErrorConstant;
import id.supriadi.quarkus.model.Person;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Path("/persons")
public class PersonResource {
    private static final Map<Integer, Person> DATABASE = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    static {
        Integer id1 = COUNTER.incrementAndGet();
        DATABASE.put(id1, new Person(id1, "Alice"));

        Integer id2 = COUNTER.incrementAndGet();
        DATABASE.put(id2, new Person(id2, "Bob"));
    }

    @GET
    public Uni<Collection<Person>> getAll() {
        return Uni.createFrom().item(DATABASE.values());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getById(@PathParam("id") Integer id) {

        Person person = DATABASE.get(id);

        if (person == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(ErrorConstant.PERSON_NOT_FOUND)
                            .build()
            );
        }

        return Uni.createFrom().item(
                Response.ok(person).build()
        );
    }

    @POST
    public Uni<Response> create(Person person) {

        Integer id = COUNTER.incrementAndGet();

        person.setId(id);
        DATABASE.put(id, person);

        return Uni.createFrom().item(
                Response.status(Response.Status.CREATED)
                        .entity(person)
                        .build()
        );
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> update(@PathParam("id") Integer id, Person updatedPerson) {

        Person existing = DATABASE.get(id);

        if (existing == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(ErrorConstant.PERSON_NOT_FOUND)
                            .build()
            );
        }

        existing.setName(updatedPerson.getName());

        return Uni.createFrom().item(
                Response.ok(existing).build()
        );
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") Integer id) {

        Person removed = DATABASE.remove(id);

        if (removed == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("Person not found")
                            .build()
            );
        }

        return Uni.createFrom().item(
                Response.noContent().build()
        );
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Person> streamPersons() {

        return Multi.createFrom()
                .ticks()
                .every(Duration.ofMillis(500))

                // stop after 100 items
                .select().first(100)

                // convert tick -> Person
                .onItem().transform(tick -> generateRandomPerson());
    }

    private static final String[] NAMES = {
            "Alice", "Bob", "Charlie", "David", "Emma",
            "Farah", "George", "Hana", "Ivan", "Julia"
    };

    private final Random random = new Random();
    private final AtomicInteger counter2 = new AtomicInteger();

    private Person generateRandomPerson() {

        Integer id = counter2.incrementAndGet();

        String name = NAMES[random.nextInt(NAMES.length)];

        return new Person(id, name);
    }
}