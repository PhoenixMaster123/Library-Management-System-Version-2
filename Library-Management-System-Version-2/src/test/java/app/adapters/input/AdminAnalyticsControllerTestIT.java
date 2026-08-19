package app.adapters.input;

import app.domain.model.LoanStatistics;
import app.domain.port.output.LoanStatisticsPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The analytics endpoint, with Analytics-Service itself stubbed out at the port.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class AdminAnalyticsControllerTestIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanStatisticsPort loanStatisticsPort;

    private static LoanStatistics statistics() {
        return new LoanStatistics(2, 9, 5, 4, true, Instant.parse("2026-08-18T10:00:00Z"), List.of(
                new LoanStatistics.BookStat(UUID.randomUUID(), "Dune", "978-0-441-01359-3", 6, 4, 2)));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void servesTheStatisticsItIsGiven() throws Exception {
        when(loanStatisticsPort.fetch(anyInt())).thenReturn(Optional.of(statistics()));

        mockMvc.perform(get("/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.booksTracked").value(2))
                .andExpect(jsonPath("$.summary.totalBorrows").value(9))
                .andExpect(jsonPath("$.summary.totalReturns").value(5))
                .andExpect(jsonPath("$.summary.currentlyOut").value(4))
                .andExpect(jsonPath("$.popularBooks[0].title").value("Dune"))
                .andExpect(jsonPath("$.popularBooks[0].timesBorrowed").value(6))
                .andExpect(jsonPath("$.summary.streamConnected").value(true));
    }

    /** 503, not zeroed statistics: the caller must be able to tell the two apart. */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void answers503WhenTheStatisticsCannotBeRead() throws Exception {
        when(loanStatisticsPort.fetch(anyInt())).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/analytics"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Analytics is unavailable."))
                .andExpect(jsonPath("$.summary").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void clampsTheRequestedLimit() throws Exception {
        when(loanStatisticsPort.fetch(anyInt())).thenReturn(Optional.of(statistics()));

        mockMvc.perform(get("/admin/analytics").param("limit", "9999")).andExpect(status().isOk());
        verify(loanStatisticsPort).fetch(eq(100));

        mockMvc.perform(get("/admin/analytics").param("limit", "0")).andExpect(status().isOk());
        verify(loanStatisticsPort).fetch(eq(1));
    }

    /**
     * A reachable Analytics-Service with no broker behind it. This answers 200, not 503 - the
     * service is fine - but the body has to say the stream is down, or the zeros read as fact.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void reportsAnEmptyProjectionAsDisconnectedRatherThanAsZero() throws Exception {
        when(loanStatisticsPort.fetch(anyInt()))
                .thenReturn(Optional.of(new LoanStatistics(0, 0, 0, 0, false, null, List.of())));

        mockMvc.perform(get("/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.streamConnected").value(false))
                .andExpect(jsonPath("$.summary.totalBorrows").value(0));
    }

    /** Inherited from the /admin/** rule in SecurityConfig, so this guards that it still holds. */
    @Test
    @WithMockUser(username = "member", roles = "USER")
    void refusesAMember() throws Exception {
        mockMvc.perform(get("/admin/analytics")).andExpect(status().isForbidden());
    }
}
