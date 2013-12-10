package at.ac.univie.isc.asio.common;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Delete all files and directories encountered.
 * 
 * @author Chris Borckholder
 */
final class PurgeVisitor implements FileVisitor<Path> {
	@Override
	public FileVisitResult preVisitDirectory(final Path dir,
			final BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file,
			final BasicFileAttributes attrs) throws IOException {
		Files.deleteIfExists(file);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file,
			final IOException exc) throws IOException {
		Files.deleteIfExists(file); // try again ...
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir,
			final IOException exc) throws IOException {
		Files.deleteIfExists(dir);
		return FileVisitResult.CONTINUE;
	}
}