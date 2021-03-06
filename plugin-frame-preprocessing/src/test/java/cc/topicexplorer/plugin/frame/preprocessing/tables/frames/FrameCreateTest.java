package cc.topicexplorer.plugin.frame.preprocessing.tables.frames;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import cc.topicexplorer.database.Database;

public class FrameCreateTest {

	@Mock(name = "database")
	private Database dbMock;

	@InjectMocks
	FrameCreate frameCreate = new FrameCreate();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testCreateTable() throws SQLException {
		this.frameCreate.setTableName();
		this.frameCreate.createTable();
		verify(this.dbMock)
				.executeUpdateQuery(
						eq("CREATE TABLE FRAME$FRAMES (FRAME_ID INTEGER(11) NOT NULL KEY AUTO_INCREMENT, DOCUMENT_ID INT, TOPIC_ID INT, FRAME VARCHAR(100), START_POSITION INT, END_POSITION INT, ACTIVE BOOLEAN NOT NULL DEFAULT 1, FRAME_TYPE VARCHAR(100))"));
	}

	@Test(expected = IllegalStateException.class)
	public void testCreateTableThrowsException() throws SQLException {
		this.frameCreate.createTable();
	}

}
