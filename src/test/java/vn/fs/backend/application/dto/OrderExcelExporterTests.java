package vn.fs.backend.application.dto;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderExcelExporterTests {

    @Test
    void exportProducesReadableXlsxWorkbook() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new OrderExcelExporter(List.of()).export(response);

        byte[] exportedWorkbook = response.getContentAsByteArray();
        assertTrue(exportedWorkbook.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportedWorkbook))) {
            assertEquals("OrderDetails", workbook.getSheetAt(0).getSheetName());
            assertEquals(1, workbook.getSheetAt(0).getPhysicalNumberOfRows());
        }
    }
}
