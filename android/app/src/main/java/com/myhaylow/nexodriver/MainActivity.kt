package com.myhaylow.nexodriver

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.myhaylow.nexodriver.decision.*
import com.myhaylow.nexodriver.finance.*
import com.myhaylow.nexodriver.lab.*
import com.myhaylow.nexodriver.profile.*
import com.myhaylow.nexodriver.uber.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

class MainActivity : Activity() {
    private lateinit var repository: ProfileRepository
    private lateinit var overlay: ShadowOverlayController
    private var diagnostics: TextView? = null
    private val listener: (LabSnapshot?) -> Unit = { runOnUiThread { renderDiagnostics(it) } }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        NexoApplication.initializeRuntime(applicationContext)
        repository = ProfileRepository(applicationContext)
        overlay = ShadowOverlayController(this); LabState.addListener(listener); showHome()
    }

    private fun showHome() {
        screen("NEXO DRIVER") {
        addView(label("Leitura local • configuração do motorista", 14f, Color.LTGRAY))
        val selected = RuntimeConfiguration.current()?.first
        addView(label("Perfil ativo: ${selected?.profile?.name ?: "Perfil padrão"}${if (selected?.automatic == true) " • agenda" else ""}", 17f))
        addView(button("Perfis") { showProfiles() }); addView(button("Agenda automática") { showSchedule() })
        addView(button("Financeiro") { showFinance() }); addView(button("Modo Lab e diagnóstico") { showLab() })
        }
    }

    private fun showProfiles() {
        screen("PERFIS", true) {
        repository.profiles().forEach { profile ->
            val active = profile.id == repository.activeProfileId()
            addView(label("${if (active) "● " else ""}${profile.name}${if (profile.destinationMode) " • Destino próprio" else ""}\nR$ ${n(profile.minPerKm)}/km • R$ ${n(profile.targetPerHour)}/h", 16f))
            addView(button("Ativar") { repository.activate(profile.id); showProfiles() })
            addView(button("Editar") { profileDialog(profile) })
            addView(button("Duplicar") { repository.duplicate(profile.id); showProfiles() })
            if (profile.id !in setOf("default", "destination")) addView(button("Excluir") { repository.delete(profile.id); showProfiles() })
        }
        addView(button("Novo perfil") { profileDialog(null) })
        }
    }

    private fun profileDialog(existing: DriverProfile?) {
        val fields = formFields(listOf("Nome" to (existing?.name ?: ""), "Mínimo R$/km" to (existing?.minPerKm ?: 1.8).toString(),
            "Meta R$/hora" to (existing?.targetPerHour ?: 40.0).toString(), "Coleta máxima (km)" to (existing?.maxPickupDistanceKm ?: 3.5).toString(),
            "Coleta máxima (min)" to (existing?.maxPickupMinutes ?: 8.0).toString()))
        AlertDialog.Builder(this).setTitle(if (existing == null) "Novo perfil" else "Editar perfil").setView(fields.first)
            .setNegativeButton("Cancelar", null).setPositiveButton("Salvar") { _, _ ->
                runCatching { repository.save(DriverProfile(existing?.id ?: UUID.randomUUID().toString(), fields.second[0].text.toString().trim(),
                    value(fields.second[1]), value(fields.second[2]), value(fields.second[3]), value(fields.second[4]), existing?.destinationMode ?: false)) }
                    .onFailure { toast("Confira os campos do perfil") }; showProfiles()
            }.show()
    }

    private fun showSchedule() {
        screen("AGENDA AUTOMÁTICA", true) {
        val toggle = Switch(this@MainActivity).apply { text = "Ativar agenda automática"; setTextColor(Color.WHITE); isChecked = repository.automaticEnabled()
            setOnCheckedChangeListener { _, checked -> repository.setAutomaticEnabled(checked) } }
        addView(toggle)
        addView(label("Sobreposições: maior prioridade; em empate, menor ID. Sem faixa válida, permanece o perfil manual.", 13f, Color.LTGRAY))
        repository.schedules().sortedBy { it.id }.forEach { schedule ->
            val name = repository.profiles().firstOrNull { it.id == schedule.profileId }?.name ?: "Perfil removido"
            val days = schedule.days.sortedBy { it.value }.joinToString(", ") { dayLabel(it) }
            val overnight = if (schedule.start > schedule.end) " • atravessa meia-noite" else ""
            addView(label("$name • $days • ${schedule.start}–${schedule.end}$overnight • prioridade ${schedule.priority}", 15f))
            addView(button("Excluir faixa") { repository.deleteSchedule(schedule.id); showSchedule() })
        }
        addView(button("Adicionar faixa") { scheduleDialog() })
        }
    }

    private fun scheduleDialog() {
        val profiles = repository.profiles().filterNot { it.destinationMode }
        val names = profiles.map { it.name }.toTypedArray(); var selected = 0
        val fields = formFields(listOf("Início (HH:mm)" to "08:00", "Fim (HH:mm)" to "18:00", "Prioridade" to "0"))
        val dayChecks = DayOfWeek.entries.associateWith { day -> CheckBox(this).apply {
            text = dayLabel(day); setTextColor(Color.WHITE); isChecked = true
        } }
        val dayBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL
            DayOfWeek.entries.chunked(4).forEach { rowDays ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    rowDays.forEach { addView(dayChecks.getValue(it)) }
                })
            }
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL
            addView(Spinner(this@MainActivity).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names); setOnItemSelectedListener(SimpleSelection { selected = it }) })
            addView(label("Dias da semana", 14f, Color.LTGRAY)); addView(dayBox)
            addView(fields.first) }
        AlertDialog.Builder(this).setTitle("Nova faixa").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Salvar") { _, _ ->
            runCatching { repository.saveSchedule(ProfileSchedule(UUID.randomUUID().toString(), profiles[selected].id,
                dayChecks.filterValues { it.isChecked }.keys,
                LocalTime.parse(fields.second[0].text), LocalTime.parse(fields.second[1].text), fields.second[2].text.toString().toInt())) }
                .onFailure { toast("Selecione dias e use horários HH:mm e prioridade inteira") }; showSchedule()
        }.show()
    }

    private fun showFinance() {
        screen("FINANCEIRO", true) {
        val c = repository.costs(); val calculated = FinanceCalculator.costPerKm(c)
        addView(label(calculated?.let { "Custo estimado: R$ ${n(it)}/km" } ?: "Custo: não configurado", 18f))
        addView(label("Preencha todos os campos para calcular. O custo informa lucro, mas não altera a decisão.", 13f, Color.LTGRAY))
        addView(button("Configurar custos") { financeDialog(c) })
        }
    }

    private fun financeDialog(c: OperatingCosts) {
        val values = listOf("Combustível R$/litro" to c.fuelPricePerLiter, "Consumo km/litro" to c.consumptionKmPerLiter,
            "Manutenção R$/km" to c.maintenancePerKm, "Pneus R$/km" to c.tiresPerKm, "Seguro mensal" to c.insuranceMonthly,
            "IPVA anual" to c.ipvaYearly, "Outros mensais" to c.otherMonthly, "Rodagem mensal (km)" to c.expectedKmMonthly)
        val fields = formFields(values.map { it.first to (it.second?.toString() ?: "") })
        AlertDialog.Builder(this).setTitle("Custos operacionais").setView(fields.first).setNegativeButton("Cancelar", null).setPositiveButton("Salvar") { _, _ ->
            fun optional(i: Int) = fields.second[i].text.toString().replace(',', '.').toDoubleOrNull()
            repository.saveCosts(OperatingCosts(optional(0), optional(1), optional(2), optional(3), optional(4), optional(5), optional(6), optional(7))); showFinance()
        }.show()
    }

    private fun showLab() {
        screen("MODO LAB", true) {
        addView(label("Diagnóstico temporário e anonimizado", 14f, Color.LTGRAY))
        addView(button("Abrir configurações de Acessibilidade") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        addView(button("Permitir sobreposição") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) })
        addView(button("Preview fictício do overlay") { showPreview() })
        diagnostics = label("Nenhuma oferta lida.", 15f).also(::addView); renderDiagnostics(LabState.current)
        addView(button("Limpar diagnósticos") { LabState.clearDiagnostics(); this@MainActivity.overlay.hide() })
        }
    }

    private fun renderDiagnostics(s: LabSnapshot?) { diagnostics?.text = s?.let { "Decisão: ${it.result.decision}\nPerfil: ${it.result.profileId}\nR$/km: ${n(it.result.metrics.perKm)}\nLucro líquido: ${it.estimatedNetProfit?.let(::n) ?: "não configurado"}" } ?: "Nenhuma oferta lida." }
    private fun showPreview() {
        val offer = UberOffer(OfferPresentation.RADAR, 24.0, "Categoria fictícia", RouteLeg(5, 2.0), RouteLeg(25, 10.0), 2.0, null, null, null, emptySet(), 0.99)
        UberDecisionBridge.consume(ParseResult.Offer(offer)); if (!isAccessibilityEnabled()) LabState.current?.let(overlay::show)
    }
    private fun isAccessibilityEnabled(): Boolean { val component = ComponentName(this, UberOfferAccessibilityService::class.java).flattenToString(); return Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty().split(':').any { it.equals(component, true) } }
    private fun screen(title: String, back: Boolean = false, content: LinearLayout.() -> Unit) { setContentView(ScrollView(this).apply { addView(LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setBackgroundColor(Color.rgb(11,14,20)); setPadding(32,32,32,48); addView(label(title, 24f, Color.rgb(150,110,255))); if (back) addView(button("← Início") { showHome() }); content() }) }) }
    private fun formFields(values: List<Pair<String,String>>): Pair<LinearLayout,List<EditText>> { val fields = values.map { (hint, initial) -> EditText(this).apply { this.hint = hint; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setText(initial); inputType = if (hint == "Nome") InputType.TYPE_CLASS_TEXT else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL } }; return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,8,24,8); fields.forEach(::addView) } to fields }
    private fun label(value: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply { text=value; textSize=size; setTextColor(color); gravity=Gravity.CENTER; setPadding(8,12,8,12) }
    private fun button(value: String, action: () -> Unit) = Button(this).apply { text=value; isAllCaps=false; setOnClickListener { action() } }
    private fun value(field: EditText) = field.text.toString().replace(',', '.').toDouble()
    private fun n(value: Double) = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value)
    private fun dayLabel(day: DayOfWeek) = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")[day.value - 1]
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    override fun onDestroy() { LabState.removeListener(listener); overlay.hide(); super.onDestroy() }
}

private class SimpleSelection(val selected: (Int) -> Unit) : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = selected(position)
    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}
