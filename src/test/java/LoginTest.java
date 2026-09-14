import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CheckPoint 5 - Automação de Testes Funcionais da Tela de Login
 * Projeto: Validação de Segurança (ISO 27001) e Conformidade QA
 * Target: https://www.saucedemo.com/
 * Tecnologias: Java 21+, JUnit 5, Selenium WebDriver 4
 */
@DisplayName("CheckPoint 5 - Automação de Testes de Login (SauceDemo)")
public class LoginTest {

    // --- Constantes de Configuração e Dados de Teste ---
    private static final String BASE_URL = "https://www.saucedemo.com/";
    private static final String USUARIO_VALIDO = "standard_user";
    private static final String SENHA_VALIDA = "secret_sauce";
    private static final String USUARIO_BLOQUEADO = "locked_out_user";
    private static final String USUARIO_INVALIDO = "usuario_invalido";
    private static final String SENHA_INVALIDA = "senha_errada";

    // --- Locators (Estratégias de Busca de Elementos no DOM) ---
    private static final By CAMPO_USUARIO = By.id("user-name");
    private static final By CAMPO_SENHA = By.id("password");
    private static final By BOTAO_LOGIN = By.id("login-button");
    private static final By ICONE_CARRINHO = By.className("shopping_cart_link");
    private static final By MENSAGEM_ERRO = By.cssSelector("[data-test='error']");

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void abrirNavegador() {
        WebDriverManager.chromedriver().setup();
        // Inicializa o Chrome via Selenium 4 (Selenium Manager baixa o driver automaticamente)
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        // Sincronização explícita com timeout de 10 segundos (Requisito Não Funcional)
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void fecharNavegador() {
        // Encerra a instância do navegador ao final de cada teste (evita processos zumbis)
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("CT1 - Login com sucesso (Status 200 OK / Caminho Feliz)")
    void deveLogarComCredenciaisValidas() {
        // Dado: que esteja na página saucedemo.com
        driver.get(BASE_URL);
        assertEquals(BASE_URL, driver.getCurrentUrl(), "A URL inicial deve corresponder à BASE_URL");
        assertEquals("Swag Labs", driver.getTitle(), "O título da página deve ser 'Swag Labs'");

        // Quando: inserir dados de usuário e senha válidos
        driver.findElement(CAMPO_USUARIO).sendKeys(USUARIO_VALIDO);
        driver.findElement(CAMPO_SENHA).sendKeys(SENHA_VALIDA);

        // E: clicar no botão "Login"
        driver.findElement(BOTAO_LOGIN).click();

        // Então: deverá ser redirecionado para a página inventory.html com elementos visíveis
        wait.until(ExpectedConditions.urlContains("inventory.html"));
        assertEquals(BASE_URL + "inventory.html", driver.getCurrentUrl(), "URL de destino incorreta pós-login");
        assertTrue(driver.findElement(ICONE_CARRINHO).isDisplayed(), "O ícone do carrinho deve estar visível no catálogo");
    }

    @Test
    @DisplayName("CT2 - Redirecionamento e verificação de URL (Status 302 / Integração de Fluxo)")
    void deveRedirecionarCorretamenteAposAutenticacao() {
        // Dado: que esteja na página de login
        driver.get(BASE_URL);

        // Quando: autenticar com sucesso
        driver.findElement(CAMPO_USUARIO).sendKeys(USUARIO_VALIDO);
        driver.findElement(CAMPO_SENHA).sendKeys(SENHA_VALIDA);
        driver.findElement(BOTAO_LOGIN).click();

        // Então: confirma que o navegador processou o redirecionamento HTTP 302 para a área autenticada
        wait.until(ExpectedConditions.visibilityOfElementLocated(ICONE_CARRINHO));
        assertTrue(driver.getCurrentUrl().endsWith("/inventory.html"), "Redirecionamento para inventory.html falhou");
    }

    @Test
    @DisplayName("CT3 - Tentativa de login sem preencher usuário (Status 400 / Sintaxe / Campos Obrigatórios)")
    void deveExibirErroQuandoUsuarioEstiverEmBranco() {
        // Dado: que esteja na página de login
        driver.get(BASE_URL);

        // Quando: preencher apenas a senha e clicar em Login
        driver.findElement(CAMPO_SENHA).sendKeys(SENHA_VALIDA);
        driver.findElement(BOTAO_LOGIN).click();

        // Então: exibe a mensagem de validação de campo obrigatório
        WebElement elementoErro = wait.until(ExpectedConditions.visibilityOfElementLocated(MENSAGEM_ERRO));
        assertTrue(elementoErro.getText().contains("Username is required"), "Mensagem de usuário obrigatório incorreta");
    }

    @Test
    @DisplayName("CT4 - Login com credenciais inválidas (Status 401 / Confidencialidade - ISO 27001)")
    void deveExibirMensagemGenericaAoInformarCredenciaisIncorretas() {
        // Dado: que esteja na página de login
        driver.get(BASE_URL);

        // Quando: inserir usuário e/ou senha incorretos
        driver.findElement(CAMPO_USUARIO).sendKeys(USUARIO_INVALIDO);
        driver.findElement(CAMPO_SENHA).sendKeys(SENHA_INVALIDA);
        driver.findElement(BOTAO_LOGIN).click();

        // Então: exibe mensagem genérica que impede a enumeração de usuários (OWASP Top 10 / Confidencialidade)
        WebElement elementoErro = wait.until(ExpectedConditions.visibilityOfElementLocated(MENSAGEM_ERRO));
        String mensagemExibida = elementoErro.getText();
        
        assertTrue(mensagemExibida.contains("Username and password do not match any user in this service"),
                "A mensagem deve ser genérica para preservar a confidencialidade do sistema");
    }

    @Test
    @DisplayName("CT5 - Tentativa de login com usuário bloqueado (Status 403 / Regra de Negócio de Acesso)")
    void deveBloquearAcessoDeUsuarioBloqueado() {
        // Dado: que esteja na página de login
        driver.get(BASE_URL);

        // Quando: inserir credenciais de um usuário explicitamente bloqueado
        driver.findElement(CAMPO_USUARIO).sendKeys(USUARIO_BLOQUEADO);
        driver.findElement(CAMPO_SENHA).sendKeys(SENHA_VALIDA);
        driver.findElement(BOTAO_LOGIN).click();

        // Então: o sistema proíbe o acesso exibindo o aviso de conta bloqueada
        WebElement elementoErro = wait.until(ExpectedConditions.visibilityOfElementLocated(MENSAGEM_ERRO));
        assertTrue(elementoErro.getText().contains("Sorry, this user has been locked out."),
                "Deveria exibir aviso de usuário bloqueado");
    }

    @Test
    @DisplayName("CT6 - Validação de tempo limite de autenticação (Status 408 / Disponibilidade < 10s)")
    void deveAutenticarDentroDoLimiteMaximoDeTempo() {
        // Dado: que esteja na página de login
        driver.get(BASE_URL);

        // Quando: iniciar o processo de autenticação medindo o tempo de resposta
        long tempoInicial = System.currentTimeMillis();

        driver.findElement(CAMPO_USUARIO).sendKeys(USUARIO_VALIDO);
        driver.findElement(CAMPO_SENHA).sendKeys(SENHA_VALIDA);
        driver.findElement(BOTAO_LOGIN).click();

        wait.until(ExpectedConditions.urlContains("inventory.html"));
        long tempoFinal = System.currentTimeMillis();
        long duracaoTotalEmSegundos = (tempoFinal - tempoInicial) / 1000;

        // Então: o tempo de resposta deve ser estritamente inferior ao RNF de 10 segundos
        assertTrue(duracaoTotalEmSegundos < 10,
                "A autenticação excedeu o limite máximo de 10 segundos estabelecido nos Requisitos Não Funcionais");
    }

    @Test
    @DisplayName("CT7 - Simulação de tentativas consecutivas incorretas (Status 429 / Segurança contra Brute Force)")
    void deveValidarComportamentoEmTentativasSucessivasMalsucedidas() {
        // Dado: que esteja na página de login
        driver.get(BASE_URL);

        // Quando: realizar 3 tentativas consecutivas com credenciais erradas (Regra de Negócio de Bloqueio do CP4)
        for (int i = 1; i <= 3; i++) {
            WebElement campoUser = driver.findElement(CAMPO_USUARIO);
            WebElement campoPass = driver.findElement(CAMPO_SENHA);

            campoUser.sendKeys(Keys.CONTROL + "a", Keys.BACK_SPACE);
            campoUser.sendKeys(USUARIO_VALIDO);

            campoPass.sendKeys(Keys.CONTROL + "a", Keys.BACK_SPACE);
            campoPass.sendKeys("senha_errada_" + i);

            driver.findElement(BOTAO_LOGIN).click();

            WebElement elementoErro = wait.until(ExpectedConditions.visibilityOfElementLocated(MENSAGEM_ERRO));
            assertTrue(elementoErro.isDisplayed(), "Mensagem de erro deve persistir na tentativa " + i);
        }

        // Então: o sistema mantém a recusa de acesso e permanece na tela inicial de login
        assertEquals(BASE_URL, driver.getCurrentUrl(), "O usuário não deve ser autenticado após múltiplas falhas");
    }
}
