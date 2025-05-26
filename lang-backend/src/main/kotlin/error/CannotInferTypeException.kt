package error

class CannotInferTypeException() : RuntimeException() {
    constructor(cause : Throwable) : this() {
        this.initCause(cause)
    }
}
