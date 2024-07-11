/*
 * Copyright 2023 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mdoc.js.worker

/*
  Copy pasted from here
  https://github.com/armanbilge/scalajs-importmap/blob/main/importmap/src/main/scala/com/armanbilge/sjsimportmap/ImportMappedIRFile.scala
 */

import org.scalajs.linker.interface.IRFile
import org.scalajs.linker.interface.unstable.IRFileImpl

object ImportMappedIRFile {
  def fromIRFile(ir: IRFile)(mapper: String => String): IRFile =
    new ImportMappedIRFileImpl(IRFileImpl.fromIRFile(ir), mapper)
}
