package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.RfidComponent;
import com.pi4j.crowpi.components.exceptions.RfidException;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * This example first waits for the user to approach two different RFID cards and writes a different instance of a {@link Person} class into
 * each card. After doing so, the application registers an event listener which reads any approached cards and outputs the read and
 * deserialized {@link Person} class. The example will then sleep for 30 seconds to give the user some time to test the two tags or approach
 * other tags before shutting down cleanly by de-registering the event listener.
 */
public class RfidApp implements Application {
    @Override
    public void execute(Context pi4j) {
        // Initialize RFID component
        final var rfid = new RfidComponent(pi4j);

        // Generate two persons which will be written to tags
        // Values taken from fakenamegenerator.com, these are not real identities :-)
        final var personA = new Person(
            "Robert", "Parson",
            "21 Southern Street, Bohemia, NY 11716",
            LocalDate.of(1948, 2, 23)
        );
        final var personB = new Person(
            "Lisa", "Knee",
            "1944 Veltri Drive, Old Harbor, AK 99643",
            LocalDate.of(2000, 8, 11)
        );

        // Wait for the user to approach a first card and write person A
        System.out.println("Please approach a first card to write person A");
        rfid.waitForNewCard(card -> {
            try {
                card.writeObject(personA);
                System.out.println("Person A was written to card " + card.getSerial());
            } catch (RfidException e) {
                System.out.println("Could not write card for person A: " + e.getMessage());
            }
        });

        // Wait for the user to approach a second card and write person B
        System.out.println("Please approach a second card to write person B");
        rfid.waitForNewCard(card -> {
            try {
                card.writeObject(personB);
                System.out.println("Person B was written to card " + card.getSerial());
            } catch (RfidException e) {
                System.out.println("Could not write card for person B: " + e.getMessage());
            }
        });

        // Register event listener to detect card in proximity
        rfid.onCardDetected(card -> {
            // Print serial number and capacity of approached card
            System.out.println("Detected card with serial " + card.getSerial() + " and capacity of " + card.getCapacity() + " bytes");

            // Read `Person` object from card and print it
            try {
                final var person = card.readObject(Person.class);
                System.out.println("Read person from card: " + person);
            } catch (RfidException e) {
                System.out.println("Could not read person from card: " + e.getMessage());
            }
        });

        // Sleep for 30 seconds to give the user some time to approach various cards
        System.out.println("Waiting 30 seconds for new RFID cards, try switching between the previously written cards...");
        sleep(30000);

        // Cleanup by unregistering the event handler
        rfid.onCardDetected(null);
    }

    /**
     * This class represents a person with a random unique identifier (UUID), first name, last name, address and date of birth.
     * There is no magic involved here, except the implementation of the {@link Serializable} interface which is needed.
     * You can read and write any kind of class from/to a RFID card as long as it is serializable.
     */
    private static final class Person implements Serializable {
        private final UUID uuid;
        private final String firstName;
        private final String lastName;
        private final String address;
        private final LocalDate dateOfBirth;

        public Person(String firstName, String lastName, String address, LocalDate dateOfBirth) {
            // Generate a random UUID for this person
            this.uuid = UUID.randomUUID();

            // Store all other attributes in this class
            this.firstName = firstName;
            this.lastName = lastName;
            this.address = address;
            this.dateOfBirth = dateOfBirth;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getAddress() {
            return address;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        /**
         * Generate a nice description for this object when converted to a string which contains all attributes
         *
         * @return Human-readable string describing this person
         */
        @Override
        public String toString() {
            return getUuid() + ": " + getFirstName() + " " + getLastName() + " @ " + getAddress() + ", born " + getDateOfBirth();
        }
    }
}
