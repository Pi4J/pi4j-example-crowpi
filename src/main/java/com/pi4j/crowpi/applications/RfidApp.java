package com.pi4j.crowpi.applications;

import com.pi4j.context.Context;
import com.pi4j.crowpi.Application;
import com.pi4j.crowpi.components.RfidComponent;
import com.pi4j.crowpi.components.exceptions.RfidException;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class RfidApp implements Application {
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

    @Override
    public void execute(Context pi4j) {
        final var rfid = new RfidComponent(pi4j);

        while (true) {
            try {
                System.out.println("Card present: " + rfid.isCardPresent());
                final var card = rfid.initializeCard();
                System.out.println("Card serial: " + card.getSerial());
                System.out.println("Card capacity: " + card.getCapacity() + " bytes");

                final var input = new Person(
                    "John", "Doe",
                    "123 Maple Street, Anytown, PA 17101",
                    LocalDate.of(1980, 1, 23)
                );

                try {
                    card.writeObject(input);
                    System.out.println(">>> Reading person...");
                    final var output = card.readObject(Person.class);
                    System.out.println(output);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                break;
            } catch (RfidException e) {
                e.printStackTrace();
            }

            sleep(1000);
            rfid.reset();
        }
    }
}
