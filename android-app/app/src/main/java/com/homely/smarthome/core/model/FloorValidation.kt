package com.homely.smarthome.core.model

/**
 * Validation shared by the floor editor and the Firestore repository.
 *
 * Floor plans are intentionally kept as metadata plus an optional image URL. The
 * grid is the source of truth for device placement, so keeping its bounds small
 * prevents an accidental floor edit from making the dashboard unusable.
 */
object FloorValidation {
    const val MIN_GRID_SIZE = 1
    const val MAX_GRID_SIZE = 64
    const val MAX_NAME_LENGTH = 80
    const val MAX_IMAGE_URL_LENGTH = 2_048
    const val NEW_OPERATION_ID = "floor:new"

    fun errorFor(floor: Floor): String? {
        val name = floor.name.trim()
        if (name.isEmpty()) return "Floor name is required."
        if (name.length > MAX_NAME_LENGTH) {
            return "Floor name must be $MAX_NAME_LENGTH characters or fewer."
        }
        if (floor.gridRows !in MIN_GRID_SIZE..MAX_GRID_SIZE) {
            return "Rows must be between $MIN_GRID_SIZE and $MAX_GRID_SIZE."
        }
        if (floor.gridColumns !in MIN_GRID_SIZE..MAX_GRID_SIZE) {
            return "Columns must be between $MIN_GRID_SIZE and $MAX_GRID_SIZE."
        }
        if (floor.sortOrder < 0) return "Display order cannot be negative."
        if (floor.imageUrl.trim().length > MAX_IMAGE_URL_LENGTH) {
            return "Image URL must be $MAX_IMAGE_URL_LENGTH characters or fewer."
        }
        return null
    }

    fun requireValid(floor: Floor) {
        val error = errorFor(floor) ?: return
        throw IllegalArgumentException(error)
    }

    fun operationId(floor: Floor): String = floor.id.ifBlank { NEW_OPERATION_ID }
}
