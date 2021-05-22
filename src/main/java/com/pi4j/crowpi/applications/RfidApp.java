package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.RfidComponent;
import com.pi4j.crowpi.components.exceptions.RfidException;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class RfidApp implements Application {
    @Override
    public void execute(Context pi4j) {
        final var rfid = new RfidComponent(pi4j);

        rfid.onCardDetected(card -> {
            System.out.println("Serial: " + card.getSerial());
            System.out.println("Capacity: " + card.getCapacity() + " bytes");

            try {
                final var input = new Person("Patrick", "Velder", "Zürich", LocalDate.of(1993, 10, 13));
                card.writeObject(input);

                final var output = card.readObject(Person.class);
                System.out.println(output);
            } catch (RfidException e) {
                e.printStackTrace();
            }
        });

        sleep(30000);
    }

    public static final class Person implements Serializable {
        private final UUID uuid;
        private final String firstName;
        private final String lastName;
        private final String address;
        private final LocalDate dateOfBirth;

        public Person(String firstName, String lastName, String address, LocalDate dateOfBirth) {
            this.uuid = UUID.randomUUID();
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

        @Override
        public String toString() {
            return getUuid() + ": " + getFirstName() + " " + getLastName() + " @ " + getAddress() + ", born " + getDateOfBirth();
        }
    }
}
