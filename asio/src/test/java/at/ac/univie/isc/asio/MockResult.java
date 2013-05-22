package at.ac.univie.isc.asio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class MockResult {

	public static final byte[] PAYLOAD = "TEST-PAYLOAD"
			.getBytes(Charsets.UTF_8);

	public static ListenableFuture<Result> successFuture() {
		final SettableFuture<Result> future = SettableFuture.create();
		future.set(new Result() {
			@Override
			public InputStream getInput() throws IOException {
				return new ByteArrayInputStream(MockResult.PAYLOAD);
			}

			@Override
			public com.google.common.net.MediaType mediaType() {
				return MockFormat.MOCK_MIME;
			}
		});
		return future;
	}
}
