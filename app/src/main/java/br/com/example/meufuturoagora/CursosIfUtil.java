package br.com.example.meufuturoagora;

import java.util.Arrays;
import java.util.List;

// Cursos técnicos integrados do IFSertãoPB – Campus Santa Luzia
// (conteúdo tirado dos Projetos Pedagógicos de Curso)
class CursosIfUtil {

    static final String INFORMATICA = "informatica";
    static final String ENERGIA = "energia";
    static final String ELETRONICA = "eletronica";

    private CursosIfUtil() {
    }

    static class Disciplina {
        final String nome;
        final String carga;
        final String descricao;

        Disciplina(String nome, String carga, String descricao) {
            this.nome = nome;
            this.carga = carga;
            this.descricao = descricao;
        }
    }

    static class Ano {
        final String titulo;
        final List<Disciplina> disciplinas;

        Ano(String titulo, Disciplina... disciplinas) {
            this.titulo = titulo;
            this.disciplinas = Arrays.asList(disciplinas);
        }
    }

    static class Curso {
        String id;

        // Card
        String nome;
        String resumo;
        String infoCard;
        int icone;

        // Cores: forte (ícone, faixas), texto (títulos), suave (círculo, curva), fundo (card)
        int corForte;
        int corTexto;
        int corSuave;
        int corFundo;

        // Detalhes
        String nomeCompleto;
        String[][] infos;
        String descricao;
        String introTrabalho;
        String[] trabalho;
        String obsDisciplinas;
        List<Ano> anos;
    }

    static List<Curso> todos() {
        return Arrays.asList(informatica(), energia(), eletronica());
    }

    static Curso porId(String id) {

        for (Curso curso : todos()) {
            if (curso.id.equals(id)) {
                return curso;
            }
        }

        return null;
    }

    // =========================
    // INFORMÁTICA (azul)
    // =========================

    private static Curso informatica() {

        Curso c = new Curso();

        c.id = INFORMATICA;
        c.nome = "Informática";
        c.resumo = "Programação, sites e aplicativos";
        c.infoCard = "3 anos • Integrado ao Médio";
        c.icone = R.drawable.ic_curso_informatica;

        c.corForte = R.color.curso_azul;
        c.corTexto = R.color.curso_azul;
        c.corSuave = R.color.curso_azul_suave;
        c.corFundo = R.color.curso_azul_fundo;

        c.nomeCompleto = "Técnico em Informática";

        c.infos = new String[][]{
                {"Forma", "Integrado ao Ensino Médio"},
                {"Eixo tecnológico", "Informação e Comunicação"},
                {"Duração", "3 anos"},
                {"Turno", "Diurno"},
                {"Carga horária", "3.568 horas (com 200 h de estágio obrigatório, que pode ser feito após o 2º ano)"}
        };

        c.descricao = "O Curso Técnico em Informática forma profissionais preparados para trabalhar com "
                + "produtos e serviços de tecnologia da informação, com foco principal em programação e "
                + "desenvolvimento de sistemas. Ao longo de três anos, o estudante conclui o Ensino Médio e, "
                + "ao mesmo tempo, aprende a criar programas, sites, aplicativos para celular e bancos de dados, "
                + "além de entender o funcionamento de computadores, redes e sistemas operacionais.\n\n"
                + "O curso também trabalha ética, responsabilidade social, visão empreendedora e segurança no "
                + "trabalho, preparando o aluno tanto para entrar no mercado de trabalho quanto para continuar "
                + "os estudos em um curso superior. Em Santa Luzia, a formação atende à demanda por profissionais "
                + "de TI na região do Vale do Sabugi e ajuda a manter esses talentos na própria região.";

        c.introTrabalho = "O técnico em Informática pode atuar em instituições públicas, empresas privadas e "
                + "organizações do terceiro setor que utilizem sistemas computacionais. Entre as possibilidades estão:";

        c.trabalho = new String[]{
                "Desenvolvimento de programas e sistemas de computador",
                "Criação e manutenção de sites, portais e aplicações web",
                "Desenvolvimento de aplicativos para dispositivos móveis",
                "Projeto e administração de bancos de dados",
                "Testes e manutenção de softwares já implantados",
                "Suporte técnico, instalação de softwares e treinamento de usuários",
                "Manutenção básica de computadores e configuração de redes",
                "Abertura do próprio negócio na área de tecnologia"
        };

        c.anos = Arrays.asList(
                new Ano("1º Ano",
                        new Disciplina("Algoritmos e Lógica de Programação", "100 h",
                                "Primeiro contato com a programação. O aluno aprende a pensar de forma lógica, "
                                        + "a construir algoritmos passo a passo e a usar estruturas de decisão, "
                                        + "repetição, vetores e funções para resolver problemas com o computador."),
                        new Disciplina("Fundamentos de Hardware", "67 h",
                                "Estuda as peças que formam um computador (processador, memória, placa de vídeo, "
                                        + "fonte e periféricos), noções de eletricidade e proteção contra surtos, "
                                        + "além de manutenção básica e instalação de sistemas operacionais."),
                        new Disciplina("Fundamentos da Informática", "67 h",
                                "Apresenta os conceitos iniciais da área: o que é informação, sistema binário, "
                                        + "diferença entre hardware e software, uso de sistemas operacionais e "
                                        + "programas de escritório (textos e planilhas) e uma visão da profissão "
                                        + "e do mercado de trabalho."),
                        new Disciplina("Sistemas Operacionais", "67 h",
                                "Estuda o funcionamento, a instalação e a configuração dos sistemas Windows e "
                                        + "Linux, incluindo uso do terminal, comandos Linux, permissões e "
                                        + "gerenciamento de arquivos.")
                ),
                new Ano("2º Ano",
                        new Disciplina("Banco de Dados", "100 h",
                                "Ensina a planejar e criar bancos de dados para guardar e organizar informações. "
                                        + "Envolve modelagem entidade-relacionamento, modelo relacional, "
                                        + "normalização e uso de um sistema gerenciador de banco de dados."),
                        new Disciplina("Programação Orientada a Objetos", "100 h",
                                "Apresenta o paradigma de orientação a objetos, muito usado no mercado. O aluno "
                                        + "trabalha conceitos como classes, objetos, herança, composição e "
                                        + "polimorfismo aplicados em uma linguagem de programação."),
                        new Disciplina("Estrutura de Dados", "67 h",
                                "Mostra formas eficientes de organizar dados dentro de um programa, como listas, "
                                        + "filas, pilhas e árvores, além de ponteiros, alocação de memória e "
                                        + "métodos de busca e ordenação."),
                        new Disciplina("Redes de Computadores", "67 h",
                                "Explica como os computadores se comunicam: topologias, cabos e redes sem fio, "
                                        + "modelo OSI e arquitetura TCP/IP, endereçamento IP e protocolos da "
                                        + "internet como HTTP, DNS e e-mail."),
                        new Disciplina("Desenvolvimento de Aplicações Web I", "67 h",
                                "Introdução à criação de sites. O aluno aprende a estruturar páginas com HTML, "
                                        + "estilizá-las com CSS e deixá-las interativas com JavaScript, usando "
                                        + "também bibliotecas e frameworks."),
                        new Disciplina("Higiene e Segurança no Trabalho", "33 h",
                                "Aborda a prevenção de acidentes e a saúde do trabalhador: riscos no ambiente de "
                                        + "trabalho, equipamentos de proteção, primeiros socorros, combate a "
                                        + "incêndios, NR-10 e noções de ergonomia.")
                ),
                new Ano("3º Ano",
                        new Disciplina("Desenvolvimento de Aplicações Web II", "100 h",
                                "Continuação de Web I, agora com foco no lado do servidor. O aluno desenvolve "
                                        + "sistemas web completos integrados a banco de dados, com login, "
                                        + "controle de sessão e autenticação de usuários."),
                        new Disciplina("Análise e Projeto de Sistemas", "67 h",
                                "Ensina a planejar um software antes de programá-lo: levantamento de requisitos, "
                                        + "fundamentos de engenharia de software, modelagem com a linguagem UML "
                                        + "e uso de ferramentas de apoio ao projeto."),
                        new Disciplina("Segurança", "33 h",
                                "Trata da proteção de sistemas e informações: políticas de segurança, senhas e "
                                        + "controle de acesso, criptografia, certificados digitais, tipos de "
                                        + "ataques e formas de defesa."),
                        new Disciplina("Tópicos Especiais em Informática", "100 h",
                                "Disciplina de ementa flexível, voltada ao estudo de temas atuais e emergentes "
                                        + "da área de Informática. Os conteúdos são definidos a critério do "
                                        + "professor, permitindo acompanhar as novas tecnologias e tendências do "
                                        + "mercado e aprofundar conhecimentos aplicados à formação técnica do "
                                        + "estudante.")
                )
        );

        return c;
    }

    // =========================
    // SISTEMAS DE ENERGIA RENOVÁVEL (amarelo)
    // =========================

    private static Curso energia() {

        Curso c = new Curso();

        c.id = ENERGIA;
        c.nome = "Sistemas de Energia Renovável";
        c.resumo = "Energia solar, eólica e biomassa";
        c.infoCard = "3 anos • Integrado ao Médio";
        c.icone = R.drawable.ic_curso_energia;

        c.corForte = R.color.curso_amarelo;
        c.corTexto = R.color.curso_amarelo_texto;
        c.corSuave = R.color.curso_amarelo_suave;
        c.corFundo = R.color.curso_amarelo_fundo;

        c.nomeCompleto = "Técnico em Sistemas de Energia Renovável";

        c.infos = new String[][]{
                {"Forma", "Integrado ao Ensino Médio"},
                {"Eixo tecnológico", "Controle e Processos Industriais"},
                {"Duração", "3 anos"},
                {"Turno", "Diurno"},
                {"Carga horária", "3.566 horas (com 200 h de estágio)"},
                {"Vigência", "A partir do ano letivo de 2024"}
        };

        c.descricao = "O Curso Técnico em Sistemas de Energia Renovável forma profissionais capazes de "
                + "projetar, instalar e fazer a manutenção de sistemas de energia limpa em residências e "
                + "comércios, como sistemas solares fotovoltaicos, eólicos e de biomassa. Durante três anos, o "
                + "estudante conclui o Ensino Médio e aprende eletricidade, instalações elétricas, eletrônica, "
                + "automação e as principais tecnologias de geração de energia renovável.\n\n"
                + "O curso nasceu da realidade de Santa Luzia e região, que recebem grandes investimentos em "
                + "usinas solares e parques eólicos por causa da alta incidência de sol e ventos e da "
                + "proximidade com uma subestação do Sistema Interligado Nacional. Esse setor precisa de "
                + "pessoas qualificadas em todas as etapas: venda, projeto, instalação e manutenção. A formação "
                + "também valoriza o desenvolvimento sustentável, a ética e a preservação do meio ambiente.";

        c.introTrabalho = "O técnico em Sistemas de Energia Renovável pode atuar em:";

        c.trabalho = new String[]{
                "Empresas de instalação, manutenção e venda de equipamentos e sistemas de energia renovável "
                        + "(solar, eólica, biomassa)",
                "Usinas solares e parques eólicos, como os instalados na região de Santa Luzia",
                "Concessionárias de energia e empresas prestadoras de serviço em geração, transmissão e "
                        + "distribuição de energia elétrica",
                "Empresas de pesquisa e de elaboração de projetos na área de energia renovável",
                "Projetos de eficiência energética e conservação de energia",
                "Abertura do próprio negócio, como empresa de instalação de sistemas fotovoltaicos"
        };

        c.anos = Arrays.asList(
                new Ano("1º Ano",
                        new Disciplina("Introdução ao Curso de Sistemas de Energia Renovável", "33 h",
                                "Apresenta o curso, os conceitos de energia e de energias renováveis, a atuação "
                                        + "do técnico no mercado de trabalho e a evolução e as perspectivas do "
                                        + "setor no Brasil e no mundo."),
                        new Disciplina("Desenho Técnico Aplicado", "67 h",
                                "Ensina a ler e produzir desenhos técnicos seguindo as normas: escalas, formatos "
                                        + "de papel, tipos de linha, cotas, vistas, cortes e perspectivas, "
                                        + "incluindo o uso de software de desenho."),
                        new Disciplina("Eletricidade e Circuitos Elétricos", "67 h",
                                "Base elétrica do curso. O aluno estuda corrente, tensão, potência e energia, "
                                        + "analisa circuitos em corrente contínua, usa instrumentos de medição e "
                                        + "monta e testa circuitos simples."),
                        new Disciplina("Energia e Meio Ambiente", "67 h",
                                "Discute as fontes de energia convencionais e alternativas, a matriz energética "
                                        + "brasileira, a relação entre energia e desenvolvimento, e temas "
                                        + "ambientais como licenciamento e avaliação de impactos."),
                        new Disciplina("Informática Aplicada", "67 h",
                                "Ensina o uso do computador no dia a dia e no trabalho: conceitos de hardware e "
                                        + "software, uso do sistema operacional e gerenciamento de arquivos, "
                                        + "editor de texto, planilha eletrônica (fórmulas, funções e gráficos), "
                                        + "programas de apresentação e uso da internet, com aplicações em "
                                        + "problemas da área de energia renovável.")
                ),
                new Ano("2º Ano",
                        new Disciplina("Instalações Elétricas", "100 h",
                                "Ensina a projetar e dimensionar instalações elétricas prediais conforme a NBR "
                                        + "5410: diagramas, condutores, eletrodutos, disjuntores, DR, DPS, "
                                        + "aterramento, quadros de distribuição e divisão de circuitos."),
                        new Disciplina("Máquinas, Comandos Elétricos e Automação", "67 h",
                                "Estuda eletromagnetismo, transformadores, motores e geradores de corrente "
                                        + "alternada e contínua, além de introdução à automação industrial, CLPs "
                                        + "e instrumentação."),
                        new Disciplina("Eletrônica Aplicada", "67 h",
                                "Apresenta os componentes eletrônicos fundamentais, como diodos, transistores e "
                                        + "amplificadores operacionais, e introduz os conversores "
                                        + "analógico-digital e digital-analógico."),
                        new Disciplina("Biocombustíveis e Biomassa", "67 h",
                                "Estuda o uso da matéria orgânica para gerar energia: tratamento da biomassa, "
                                        + "processos como combustão, pirólise, gaseificação, fermentação e "
                                        + "biodigestão, e a produção de biodiesel, bioetanol e biogás."),
                        new Disciplina("Higiene e Segurança no Trabalho", "33 h",
                                "Aborda a prevenção de acidentes e a saúde do trabalhador, com normas de "
                                        + "segurança, equipamentos de proteção e cuidados essenciais para quem "
                                        + "trabalha com eletricidade.")
                ),
                new Ano("3º Ano",
                        new Disciplina("Energia Solar", "100 h",
                                "Disciplina central do curso. Estuda a radiação solar, o funcionamento das "
                                        + "células e módulos fotovoltaicos, sistemas conectados à rede e isolados "
                                        + "(off-grid), estruturas de fixação, componentes de proteção e o projeto "
                                        + "de sistemas fotovoltaicos."),
                        new Disciplina("Sistema de Potência, Proteção e Distribuição de Energia", "100 h",
                                "Explica como a energia chega às casas: sistema elétrico de potência, "
                                        + "subestações, redes de distribuição urbanas e rurais, postes e "
                                        + "estruturas, equipamentos de manobra e proteção e ensaios elétricos."),
                        new Disciplina("Energia Eólica e Hidráulica", "67 h",
                                "Estuda o aproveitamento dos ventos e da água para gerar energia: medição do "
                                        + "vento, aerodinâmica, escolha do local, aerogeradores autônomos e "
                                        + "conectados à rede e avaliação da produção de energia."),
                        new Disciplina("Planejamento e Controle da Manutenção", "67 h",
                                "Ensina a organizar a manutenção de equipamentos e sistemas: manutenção "
                                        + "corretiva, preventiva e preditiva, planejamento, programação, "
                                        + "ferramentas e indicadores de manutenção.")
                )
        );

        return c;
    }

    // =========================
    // ELETRÔNICA (verde)
    // =========================

    private static Curso eletronica() {

        Curso c = new Curso();

        c.id = ELETRONICA;
        c.nome = "Eletrônica";
        c.resumo = "Circuitos, placas e indústria";
        c.infoCard = "3 anos • Integrado ao Médio";
        c.icone = R.drawable.ic_curso_eletronica;

        c.corForte = R.color.curso_verde;
        c.corTexto = R.color.curso_verde;
        c.corSuave = R.color.curso_verde_suave;
        c.corFundo = R.color.curso_verde_fundo;

        c.nomeCompleto = "Técnico em Eletrônica";

        c.infos = new String[][]{
                {"Forma", "Integrado ao Ensino Médio"},
                {"Área / eixo", "Indústria – Controle e Processos Industriais"},
                {"Duração", "3 anos"},
                {"Carga horária", "3.767 horas + 360 h de estágio supervisionado ou TCC"},
                {"Local de oferta", "Santa Luzia – PB"}
        };

        c.descricao = "O Curso Técnico em Eletrônica forma profissionais para atuar no projeto, instalação e "
                + "manutenção de equipamentos e sistemas eletrônicos, sempre respeitando normas técnicas e de "
                + "segurança. O estudante conclui o Ensino Médio enquanto aprende eletricidade, eletrônica "
                + "analógica e digital, montagem de circuitos, eletrônica industrial, instalações elétricas e "
                + "montagem e manutenção de computadores.\n\n"
                + "A formação une bases científicas, tecnológicas e humanísticas, estimulando o pensamento "
                + "crítico, a visão empreendedora e a consciência ambiental e social. O profissional sai "
                + "preparado para realizar medições e testes, atuar no controle de qualidade e na produção de "
                + "equipamentos eletrônicos e acompanhar a chegada de novas tecnologias.";

        c.introTrabalho = "O técnico em Eletrônica pode atuar em:";

        c.trabalho = new String[]{
                "Indústrias em geral",
                "Laboratórios de controle de qualidade e de manutenção",
                "Empresas de informática, telecomunicações e fabricantes de produtos eletrônicos",
                "Manutenção de equipamentos de telefonia, equipamentos médico-hospitalares, sistemas "
                        + "digitais e sistemas de segurança",
                "Empresas de geração de energia elétrica e do setor da construção civil",
                "Venda e administração de produtos eletrônicos e prestação de serviços em eletrônica",
                "Abertura do próprio negócio na área"
        };

        c.anos = Arrays.asList(
                new Ano("1º Ano",
                        new Disciplina("Eletricidade Básica", "100 h",
                                "Base de todo o curso, trabalhada em conjunto com a Física. Estuda a natureza da "
                                        + "eletricidade, fontes, grandezas elétricas, Lei de Ohm, potência, "
                                        + "análise e medição de circuitos em corrente contínua, magnetismo e "
                                        + "eletromagnetismo."),
                        new Disciplina("Tecnologia Mecânica", "100 h",
                                "Apresenta os processos de fabricação usados na indústria, como fundição, "
                                        + "conformação mecânica dos metais e soldagem, com atividades práticas "
                                        + "em laboratório."),
                        new Disciplina("Montagem de Microcomputadores", "133 h",
                                "Ensina a arquitetura dos computadores e microprocessadores, memórias, discos, "
                                        + "placas de vídeo, interfaces, impressoras e redes, com prática de "
                                        + "montagem, manutenção e confecção de cabos.")
                ),
                new Ano("2º Ano",
                        new Disciplina("Eletrônica Básica", "167 h",
                                "Estuda os principais componentes eletrônicos, como diodos, transistores, "
                                        + "componentes optoeletrônicos e amplificadores operacionais, além de "
                                        + "circuitos amplificadores, osciladores e de instrumentação."),
                        new Disciplina("Circuitos Lógicos", "100 h",
                                "Introdução à eletrônica digital. O aluno identifica e usa circuitos integrados "
                                        + "digitais, monta circuitos de lógica combinacional e sequencial e "
                                        + "projeta circuitos com lógica programável (PLD)."),
                        new Disciplina("Periféricos de Microcomputadores", "133 h",
                                "Estuda as portas de entrada e saída do computador, a comunicação serial e "
                                        + "paralela e os periféricos básicos, finalizando com o projeto de um "
                                        + "periférico próprio.")
                ),
                new Ano("3º Ano",
                        new Disciplina("Análise de Circuitos", "67 h",
                                "Aprofunda a análise de circuitos elétricos com as Leis de Kirchhoff, "
                                        + "transformação de fontes, teorema da superposição e teoremas de "
                                        + "Thévenin e Norton."),
                        new Disciplina("Projetos Eletrônicos", "100 h",
                                "O aluno desenvolve um equipamento eletrônico do início ao fim: concepção, "
                                        + "montagem e testes em protoboard, desenho do layout e confecção da "
                                        + "placa de circuito impresso e relatório final."),
                        new Disciplina("Projetos Digitais", "100 h",
                                "Semelhante a Projetos Eletrônicos, mas com foco em circuitos lógicos: definição "
                                        + "do projeto, testes em protoboard, montagem em placa de circuito "
                                        + "impresso e avaliação do funcionamento."),
                        new Disciplina("Máquinas e Instalações Elétricas", "100 h",
                                "Estuda o dimensionamento de instalações elétricas (iluminação, tomadas, "
                                        + "condutores, proteção, aterramento e DPS) e o funcionamento de motores, "
                                        + "geradores e transformadores."),
                        new Disciplina("Eletrônica Industrial", "133 h",
                                "Estuda os dispositivos eletrônicos usados na indústria, como sensores e "
                                        + "tiristores, circuitos de disparo, conversores CA/CC e CC/CA, "
                                        + "inversores de frequência, soft-starters e controle de máquinas "
                                        + "elétricas.")
                )
        );

        return c;
    }
}
