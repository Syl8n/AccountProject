package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;


    @Test
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                                .accountUser(user)
                                .balance(0L)
                        .accountNumber("1000000012").build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void delete_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void delete_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 불일치 - 계좌 해지 실패")
    void delete_UserNotAccountOwner() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        AccountUser giri = AccountUser.builder()
                .id(13L)
                .name("Giri").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(giri)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 있음 - 계좌 해지 실패")
    void delete_BalanceNotZero() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(10L)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.ACCOUNT_HAS_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 - 계좌 해지 실패")
    void delete_AccountAlreadyUnregistered() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1000000012").build()));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }
    @Test
    void createAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", accountDto.getAccountNumber());
    }

    @Test
    void createFirstAccount() {
        //given
        AccountUser user = AccountUser.builder()
                .id(15L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000013", accountDto.getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void create_userNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder()
                .id(15L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));
        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 성공")
    void testXXX() {
        //given
        given(accountRepository.findById(anyLong()))
                .willReturn(Optional.of(Account.builder()
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("65789")
                        .build()));
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        //when
        Account account = accountService.getAccount(4555L);
        //then
        verify(accountRepository, times(1)).findById(captor.capture());
        verify(accountRepository, times(0)).save(any());
        assertEquals(4555L, captor.getValue());
        assertNotEquals(45551L, captor.getValue());
        assertTrue(4555l == captor.getValue());
        assertEquals("65789", account.getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, account.getAccountStatus());
    }

    @Test
    @DisplayName("계좌 조회 실패 - 음수로 조회")
    void testFailedToSearchAccount() {
        //given
        //when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.getAccount(-10L));
        //then
        assertEquals("Minus", exception.getMessage());
    }

    @Test
    void successGetAccountsByUserId() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pobi").build();
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountNumber("1111111111")
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountNumber("2222222222")
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountNumber("3333333333")
                        .balance(3000L)
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountInfo> accountInfos = accountService.getAccountsByUserId(1L);
        //then
        assertEquals(3, accountInfos.size());
        assertEquals("1111111111", accountInfos.get(0).getAccountNumber());
        assertEquals(1000L, accountInfos.get(0).getBalance());
        assertEquals("2222222222", accountInfos.get(1).getAccountNumber());
        assertEquals(2000L, accountInfos.get(1).getBalance());
        assertEquals("3333333333", accountInfos.get(2).getAccountNumber());
        assertEquals(3000L, accountInfos.get(2).getBalance());
    }

    @Test
    void failedGetAccountByUserId() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        //then
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

}