package net.mbonnin.vespene

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import net.mbonnin.vespene.cli.Data
import net.mbonnin.vespene.cli.TransitionRepositoryInput
import org.junit.Test

class MoshiTest {
  @Test
  fun testMoshi() {
    val data = Data(TransitionRepositoryInput(listOf("1"), true))

    val moshi = Moshi.Builder().build()

    val type = Types.newParameterizedType(Data::class.java, TransitionRepositoryInput::class.java)
    val adapter = moshi.adapter<Data<TransitionRepositoryInput>>(type)

    val json = adapter.toJson(data)

    val data2 = adapter.fromJson(json)

    assert(data2?.data?.stagedRepositoryIds?.first() == "1")
  }
}