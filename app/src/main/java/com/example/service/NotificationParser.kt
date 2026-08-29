package com.example.service

import com.example.data.model.ExpenseCategory
import java.util.regex.Pattern

data class ParsedNotificationResult(
    val amount: Double,
    val description: String,
    val category: ExpenseCategory,
    val originalText: String
)

object NotificationParser {

    // Regex for Brazilian Real amounts:
    // Examples: "R$ 1.250,50", "R$45,90", "R$ 100", "45,90 reais", "valor: R$ 34,20"
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:R\$\s*|valor\s*(?:de)?\s*R?\$\s*|quantia\s*de\s*R?\$\s*)([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:[,\.][0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val REAIS_PATTERN = Pattern.compile(
        """([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:[,\.][0-9]{1,2})?)\s*(?:reais|BRL)""",
        Pattern.CASE_INSENSITIVE
    )

    // Financial intent keywords
    private val FINANCIAL_KEYWORDS = listOf(
        "compra", "aprovada", "realizada", "pix", "transferência", "transferencia",
        "pagamento", "débito", "debito", "crédito", "credito", "fatura", "gasto",
        "cartão", "cartao", "pago", "enviado", "recebido", "banco", "conta"
    )

    fun isFinancialNotification(title: String?, text: String?): Boolean {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".lowercase()
        val hasKeyword = FINANCIAL_KEYWORDS.any { combined.contains(it) }
        val hasAmount = AMOUNT_PATTERN.matcher(combined).find() || REAIS_PATTERN.matcher(combined).find()
        return hasKeyword && hasAmount
    }

    fun parse(title: String?, text: String?): ParsedNotificationResult? {
        val rawTitle = title.orEmpty()
        val rawText = text.orEmpty()
        val combined = "$rawTitle $rawText".trim()

        if (combined.isBlank()) return null

        var extractedAmount: Double? = null

        // Try primary amount matcher
        val matcher = AMOUNT_PATTERN.matcher(combined)
        if (matcher.find()) {
            val amountStr = matcher.group(1)
            extractedAmount = parseDoubleAmount(amountStr)
        } else {
            val reaisMatcher = REAIS_PATTERN.matcher(combined)
            if (reaisMatcher.find()) {
                val amountStr = reaisMatcher.group(1)
                extractedAmount = parseDoubleAmount(amountStr)
            }
        }

        if (extractedAmount == null || extractedAmount <= 0.0) {
            return null
        }

        val description = extractDescription(rawTitle, rawText, combined)
        val category = inferCategory(description, combined)

        return ParsedNotificationResult(
            amount = extractedAmount,
            description = description,
            category = category,
            originalText = combined
        )
    }

    private fun parseDoubleAmount(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return try {
            var clean = raw.trim()
            // If format is like 1.250,50 -> remove dots and replace comma with dot
            if (clean.contains(".") && clean.contains(",")) {
                clean = clean.replace(".", "").replace(",", ".")
            } else if (clean.contains(",")) {
                clean = clean.replace(",", ".")
            }
            clean.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDescription(title: String, text: String, combined: String): String {
        val lowerText = text.lowercase()

        // Match "no/na/em/para [Estabelecimento]"
        val establishmentRegex = Regex(
            """(?:no|na|em|para|de)\s+([A-Za-z0-9\u00C0-\u017F\s\.\-&]{2,30}?)(?:\s+no\s+valor|\s+de\s+R\$|\s+R\$|\.|\,|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        val match = establishmentRegex.find(text)
        if (match != null) {
            val candidate = match.groupValues[1].trim()
            if (candidate.isNotBlank() && !isStopWord(candidate)) {
                return candidate.replaceFirstChar { it.uppercase() }
            }
        }

        // Check common apps/establishments
        val commonNames = listOf(
            "iFood", "Uber", "99 App", "Mercado Livre", "Amazon", "Shopee",
            "Supermercado", "Padaria", "Farmácia", "Netflix", "Spotify",
            "Shell", "Ipiranga", "Smart Fit", "McDonalds", "Burger King",
            "Pix", "Boleto", "Cartão de Crédito"
        )
        for (name in commonNames) {
            if (combined.contains(name, ignoreCase = true)) {
                return name
            }
        }

        // Fallback: use title or simplified text
        if (title.isNotBlank() && !title.equals("Android", ignoreCase = true) && !title.equals("Sistema", ignoreCase = true)) {
            return title.trim()
        }

        return "Despesa Detectada"
    }

    private fun isStopWord(word: String): Boolean {
        val stops = setOf("cartão", "cartao", "conta", "banco", "valor", "reais", "aprovada", "pix")
        return stops.contains(word.lowercase())
    }

    private fun inferCategory(description: String, fullText: String): ExpenseCategory {
        val combined = "$description $fullText".lowercase()

        return when {
            listOf("ifood", "rappi", "mcdonald", "burger", "restaurante", "lanchonete", "padaria", "mercado", "supermercado", "carrefour", "pão de açúcar", "extra", "hortifruti", "açougue", "pizza", "café", "almoço", "jantar", "refeição").any { combined.contains(it) } -> ExpenseCategory.ALIMENTACAO
            listOf("uber", "99", "taxi", "posto", "shell", "ipiranga", "combustivel", "gasolina", "estacionamento", "pedagio", "pedágio", "sem parar", "veloe", "passagem", "metrô", "onibus").any { combined.contains(it) } -> ExpenseCategory.TRANSPORTE
            listOf("aluguel", "condomínio", "condominio", "enel", "sabesp", "copel", "cemig", "cpfl", "luz", "água", "agua", "gás", "gas", "comgás", "internet", "claro", "vivo", "tim", "fibra").any { combined.contains(it) } -> ExpenseCategory.CONTAS
            listOf("netflix", "spotify", "cinema", "ingresso", "steam", "playstation", "xbox", "disney", "hbo", "prime video", "show", "teatro", "jogos", "bar", "cerveja", "lazer").any { combined.contains(it) } -> ExpenseCategory.LAZER
            listOf("farmacia", "farmácia", "droga raia", "drogasil", "pague menos", "drogaria", "consulta", "laboratório", "exame", "médico", "dentista", "unimed", "hospital", "saúde").any { combined.contains(it) } -> ExpenseCategory.SAUDE
            listOf("udemy", "alura", "curso", "livro", "faculdade", "escola", "mensalidade", "educação").any { combined.contains(it) } -> ExpenseCategory.EDUCACAO
            listOf("amazon", "mercado livre", "shopee", "shein", "magalu", "magazine", "aliexpress", "zara", "renner", "riachuelo", "c&a", "loja", "compras").any { combined.contains(it) } -> ExpenseCategory.COMPRAS
            else -> ExpenseCategory.OUTROS
        }
    }
}
