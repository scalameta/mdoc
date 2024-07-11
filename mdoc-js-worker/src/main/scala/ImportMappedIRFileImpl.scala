// /*
//  * Copyright 2023 Arman Bilge
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package mdoc.js.worker

// /*
// Copy pasted from here;
// https://github.com/armanbilge/scalajs-importmap/blob/main/importmap/src/main/scala/com/armanbilge/sjsimportmap/ImportMappedIRFileImpl.scala
//  */

// import org.scalajs.ir.Trees
// import org.scalajs.linker.interface.unstable.IRFileImpl

// import scala.concurrent.ExecutionContext
// import scala.concurrent.Future

// private final class ImportMappedIRFileImpl(impl: IRFileImpl, mapper: String => String)
//     extends IRFileImpl(impl.path, impl.version) {

//   def entryPointsInfo(implicit ec: ExecutionContext) =
//     impl.entryPointsInfo

//   def tree(implicit ec: ExecutionContext): Future[Trees.ClassDef] =
//     impl.tree.map { classDef =>
//       if (classDef.jsNativeLoadSpec.isDefined || classDef.jsNativeMembers.nonEmpty)
//         Trees.ClassDef(
//           classDef.name,
//           classDef.originalName,
//           classDef.kind,
//           classDef.jsClassCaptures,
//           classDef.superClass,
//           classDef.interfaces,
//           classDef.jsSuperClass,
//           classDef.jsNativeLoadSpec.map(transform),
//           classDef.fields,
//           classDef.methods,
//           classDef.jsConstructor,
//           classDef.jsMethodProps,
//           classDef.jsNativeMembers.map(transform),
//           classDef.topLevelExportDefs
//         )(classDef.optimizerHints)(classDef.pos)
//       else classDef
//     }

//   private[this] def transform(member: Trees.JSNativeMemberDef): Trees.JSNativeMemberDef =
//     Trees.JSNativeMemberDef(
//       member.flags,
//       member.name,
//       transform(member.jsNativeLoadSpec)
//     )(member.pos)

//   private[this] def transform(spec: Trees.JSNativeLoadSpec): Trees.JSNativeLoadSpec =
//     spec match {
//       case importSpec: Trees.JSNativeLoadSpec.Import => transform(importSpec)
//       case Trees.JSNativeLoadSpec.ImportWithGlobalFallback(importSpec, globalSpec) =>
//         Trees.JSNativeLoadSpec.ImportWithGlobalFallback(transform(importSpec), globalSpec)
//       case other => other
//     }

//   private[this] def transform(
//       importSpec: Trees.JSNativeLoadSpec.Import
//   ): Trees.JSNativeLoadSpec.Import =
//     importSpec.copy(module = mapper(importSpec.module))

// }
