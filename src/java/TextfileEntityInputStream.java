package goophi.textfileentity;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class TextfileEntityInputStream extends FilterInputStream
{
	public static int TABSTOP = 4;
	public static int BUFFERSIZE = 81920;

	private enum State
	{
		NEWLINE,
		FILL,
		FILL_AND_STRIP,
		CLOSED
	}

	private State state = State.NEWLINE;
	private ByteArrayOutputStream out = new ByteArrayOutputStream();

	public TextfileEntityInputStream(InputStream in)
	{
		super(in);
	}

	@Override
	public int available() throws IOException
	{
		fillBuffer();
		
		return out.size();
	}

	@Override
	public void close() throws IOException
	{
		state = State.CLOSED;

		in.close();
		out.reset();
	}

	@Override
	public void mark(int readlimit) { }

	@Override
	public boolean markSupported()
	{
		return false;
	}

	@Override
	public int read() throws IOException
	{
		int b = -1;

		fillBuffer();
		
		if(out.size() > 0)
		{
			byte[] buffer = out.toByteArray();

			b = buffer[0];

			out.reset();

			if(buffer.length > 1)
			{
				out.write(buffer, 1, buffer.length - 1);
			}
		}

		return b;
	}

	@Override
	public int read(byte[] dst) throws IOException
	{
		return read(dst, 0, dst.length);
	}

	@Override
	public int read(byte[] dst, int offset, int len) throws IOException
	{
		int read = 0;

		if(len > 0)
		{
			fillBuffer();

			if(out.size() > 0)
			{
				byte[] buffer = out.toByteArray();

				read = Math.min(buffer.length, len);

				System.arraycopy(buffer, 0, dst, offset, read);

				out.reset();

				int bytesLeft = buffer.length - read;

				out.write(buffer, read, bytesLeft);
			}
			else
			{
				read = -1;
			}
		}

		return read;
	}

	@Override
	public void reset() throws IOException
	{
		throw new IOException("Stream doesn't support reset().");
	}

	@Override
	public long skip(long n) throws IOException
	{
		long skipped = Math.min(n, out.size());

		byte[] original = out.toByteArray();
		byte[] bytesLeft = Arrays.copyOf(original, (int)skipped);

		out.reset();
		out.write(bytesLeft);

		return skipped;
	}

	private void fillBuffer() throws IOException
	{
		while(state != State.CLOSED && out.size() < BUFFERSIZE)
		{
			step();
		}
	}

	private void step() throws IOException
	{
		if(state != State.CLOSED)
		{
			if(in.available() == 0)
			{
				endOfFile();
			}
			else
			{
				writeNextByte();
			}
		}
	}

	private void endOfFile() throws IOException
	{
		if (state != State.NEWLINE)
		{
			out.write("\r\n".getBytes());
		}

		state = State.CLOSED;

		in.close();
		out.write(".\r\n".getBytes());
	}

	void writeNextByte() throws IOException
	{
		int b = in.read();
		
		if(state == State.NEWLINE)
		{
			beginNewLine(b);
		}
		else if(b == '\n')
		{
			endOfLine();
		}
		else if(state == State.FILL || (state == State.FILL_AND_STRIP && b != '.'))
		{
			write(b);
		}
	}

	void beginNewLine(int b) throws IOException
	{
		if(b == '.')
		{
			out.write("..".getBytes());
			state = State.FILL_AND_STRIP;
		}
		else
		{
			write(b);
			state = State.FILL;
		}
	}

	void endOfLine() throws IOException
	{
		out.write("\r\n".getBytes());
		state = State.NEWLINE;
	}

	void write(int b) throws IOException
	{
		if(b == '\t')
		{
			expandTab();
		}
		else if(b != '\r')
		{
			if(Character.isISOControl((char)b))
			{
				out.write(' ');
			}
			else
			{
				out.write(b);
			}
		}

		state = State.FILL;
	}

	void expandTab() throws IOException
	{
		for(int i = 0; i < TABSTOP; ++i)
		{
			out.write(' ');
		}
	}
}
