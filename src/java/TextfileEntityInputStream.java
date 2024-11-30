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
		BEGIN_NEWLINE,
		FILL,
		STRIP_AND_FILL,
		CLOSED
	}

	private State state = State.BEGIN_NEWLINE;
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

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
        fillBuffer();

        long skipped = Math.min(n, out.size());

        if(skipped < out.size())
        {
            byte[] original = out.toByteArray();
            byte[] bytesLeft = Arrays.copyOfRange(original, (int)skipped, original.length);

            out.reset();
            out.write(bytesLeft);
        }
        else
        {
            out.reset();
        }

		return skipped;
	}

	private void fillBuffer() throws IOException
	{
		while(state != State.CLOSED)
		{
			if(out.size() > BUFFERSIZE)
			{
				throw new IOException("Buffer overflow.");
			}

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
		if (state != State.BEGIN_NEWLINE)
		{
			writeNewLine();
		}

		out.write('.');
		writeNewLine();

		in.close();

		state = State.CLOSED;
	}

	void writeNextByte() throws IOException
	{
		int b = in.read();

		switch(state)
		{
			case BEGIN_NEWLINE:
				beginNewLine(b);
				break;

			case FILL:
				fillLine(b);
				break;

			case STRIP_AND_FILL:
				stripAndFillLine(b);
				break;

			default:
				throw new IllegalStateException();
		}
	}

	void beginNewLine(int b) throws IOException
	{
		if(b == '.')
		{
			out.write('.');
			out.write('.');

			state = State.STRIP_AND_FILL;
		}
		else
		{
			write(b);

			state = State.FILL;
		}
	}

	void fillLine(int b) throws IOException
	{
		write(b);
	}

	void stripAndFillLine(int b) throws IOException
	{
		if(b != '.')
		{
			write(b);

			state = State.FILL;
		}
	}

	void write(int b) throws IOException
	{
		if(b == '\n')
		{
			writeNewLine();

			state = State.BEGIN_NEWLINE;
		}
		else if(b == '\t')
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
	}

	void writeNewLine() throws IOException
	{
		out.write('\r');
		out.write('\n');
	}

	void expandTab() throws IOException
	{
		for(int i = 0; i < TABSTOP; ++i)
		{
			out.write(' ');
		}
	}
}
