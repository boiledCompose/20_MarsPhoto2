package com.example.marsphotos.data

import com.example.marsphotos.model.MarsPhoto
import com.example.marsphotos.network.MarsApiService

/*Repository Naming Rule : Data Type + Repository*/
interface MarsPhotosRepository {
    /*Fetches list of MarsPhoto from marsApi*/
    suspend fun getMarsPhotos() : List<MarsPhoto>
}

class NetworkMarsPhotosRepository(
    private val marsApiService: MarsApiService
) : MarsPhotosRepository {
    override suspend fun getMarsPhotos(): List<MarsPhoto> = marsApiService.getPhotos()


}

