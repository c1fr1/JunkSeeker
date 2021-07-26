import Settings.CLUSTER_SIZE
import Settings.INTERVAL
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*

class FileTree(val path : String, val name : String) {
	val files : ArrayList<File>
	val dirs : ArrayList<FileTree>
	var size : Long
	var length : Int

	var sizeOnDisk : Long

	var parent : FileTree? = null
	init {
		var totalSize = 0L
		var totalSizeOnDisk = 0L
		files = ArrayList()
		dirs = ArrayList()
		val dirFiles = opendir(path)
		readdir(dirFiles)?.pointed?.name()
		readdir(dirFiles)?.pointed?.name()
		var name = readdir(dirFiles)?.pointed?.name()


		while (name != null) {
			val isDir = memScoped {
				val statBuf = alloc<stat>()
				stat("$path/$name", statBuf.ptr)
				statBuf.st_mode.toInt() and S_IFDIR != 0
			}

			totalSize += if (isDir) {
				val match = FileTree("$path/$name", name)
				match.parent = this
				dirs.add(match)
				totalSizeOnDisk += match.sizeOnDisk
				match.size
			} else {
				val fsize = memScoped {
					val fp = CreateFileA(
						"$path/$name",
						GENERIC_READ,
						FILE_SHARE_DELETE,
						null,
						OPEN_EXISTING,
						FILE_ATTRIBUTE_NORMAL,
						null
					)
					val sizeBuff = alloc<LARGE_INTEGER>()
					GetFileSizeEx(fp, sizeBuff.ptr)
					val ret = sizeBuff.QuadPart
					CloseHandle(fp)
					ret
				}
				val file = File(name, fsize)
				files.add(file)
				totalSizeOnDisk += file.sizeOnDisk
				file.size
			}
			name = readdir(dirFiles)?.pointed?.name()
		}
		closedir(dirFiles)

		filesRead += files.size
		if (filesRead > nextThreshold) {
			nextThreshold += INTERVAL
			println("$filesRead files have been discovered")
		}

		size = totalSize
		sizeOnDisk = totalSizeOnDisk
		dirs.sortByDescending { it.sizeOnDisk }
		files.sortByDescending { it.sizeOnDisk }
		length = files.size + dirs.size
	}

	fun getName(i : Int, maxLen : Int = 0) : String {
		return if (i < dirs.size) {
			"${dirs[i].name}/".padEnd(maxLen, ' ')
		} else {
			files[i - dirs.size].name.padEnd(maxLen, ' ')
		}
	}

	fun getSize(i : Int) : Long {
		return if (i < dirs.size) {
			dirs[i].size
		} else {
			files[i - dirs.size].size
		}
	}

	fun getSizeOnDisk(i : Int) : Long {
		return if (i < dirs.size) {
			dirs[i].sizeOnDisk
		} else {
			files[i - dirs.size].sizeOnDisk
		}
	}

	fun getFormattedSize(i : Int) : String {
		val portion = getSize(i).toDouble() / size.toDouble()
		if (portion < 0.001) {
			return "0.0  "
		}
		return "$portion      ".substring(0, 5)
	}

	fun getFormattedSizeOnDisk(i : Int) : String {
		val portion = getSizeOnDisk(i).toDouble() / sizeOnDisk.toDouble()
		if (portion < 0.001) {
			return "0.0  "
		}
		return "$portion      ".substring(0, 5)
	}

	fun printFiles() {
		println(path)
		println("Logical size  - ${(size / 10000000).toDouble() / 100.0} GB ($size B)")
		println("Physical size - ${(sizeOnDisk / 10000000).toDouble() / 100.0} GB ($sizeOnDisk B)")
		println()

		val maxFileNameLen = (files.maxOfOrNull { it.name.length } ?: 0).coerceAtLeast(dirs.maxOfOrNull { it.name.length } ?: 0)
		for (i in 0 until length) {
			println("${getFormattedSizeOnDisk(i)} " +
					"${getFormattedSize(i)} " +
					"${getName(i, maxFileNameLen + 1)} " +
					"${getSize(i)}")
		}
	}

	fun deleteFile(i : Int) : Boolean {
		if (remove("$path/${files[i].name}") != 0) {
			return false
		}
		size -= files[i].size
		sizeOnDisk -= files[i].sizeOnDisk
		--length
		files.removeAt(i)
		return true
	}

	fun deleteFolder(i : Int) : Boolean {
		if (!dirs[i].deleteContents()) return false
		if (rmdir(dirs[i].path) != 0) return false
		size -= dirs[i].size
		sizeOnDisk -= dirs[i].sizeOnDisk
		--length
		dirs.removeAt(i)
		return true
	}

	fun deleteContents() : Boolean {
		for (i in files.indices.reversed()) {
			if (!deleteFile(i)) return false
		}
		for (i in dirs.indices.reversed()) {
			if (!deleteFolder(i)) return false
		}
		return true
	}

	@ThreadLocal
	companion object {
		var filesRead = 0
		var nextThreshold = 0
		fun start(path : String) : FileTree {
			nextThreshold = INTERVAL
			return FileTree(path, "root")
		}
	}
}

class File(val name : String, val size : Long) {
	val sizeOnDisk = CLUSTER_SIZE * ((size + CLUSTER_SIZE - 1) / CLUSTER_SIZE)
}

fun dirent.name() = ByteArray(d_namlen.toInt()) { d_name[it] }.decodeToString()