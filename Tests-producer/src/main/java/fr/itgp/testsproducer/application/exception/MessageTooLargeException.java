package fr.itgp.testsproducer.application.exception;

public class MessageTooLargeException extends RuntimeException {

    public MessageTooLargeException(int actualSize, int maximumSize) {
        super(
                "Le message final fait %d octets ; la limite est %d"
                        .formatted(actualSize, maximumSize)
        );
    }
}
