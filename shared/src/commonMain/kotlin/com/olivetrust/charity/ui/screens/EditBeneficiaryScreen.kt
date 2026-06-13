package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.FamilyMember
import com.olivetrust.charity.ui.previews.PreviewMocks

class EditBeneficiaryScreen(private val initialBeneficiary: Beneficiary) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<OnboardingViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(state) {
            if (state is OnboardingState.Success) {
                navigator.pop()
            }
        }

        EditBeneficiaryContent(
            initialBeneficiary = initialBeneficiary,
            state = state,
            onBack = { navigator.pop() },
            onSubmit = { viewModel.submit(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBeneficiaryContent(
    initialBeneficiary: Beneficiary,
    state: OnboardingState,
    onBack: () -> Unit,
    onSubmit: (Beneficiary) -> Unit
) {
    val focusManager = LocalFocusManager.current

    // State
    var headName by remember { mutableStateOf(initialBeneficiary.headName) }
    var headAge by remember { mutableStateOf(initialBeneficiary.headAge.toString()) }
    var headGender by remember { mutableStateOf(initialBeneficiary.headGender) }
    var headOccupation by remember { mutableStateOf(initialBeneficiary.headOccupation) }
    var headEducation by remember { mutableStateOf(initialBeneficiary.headEducation) }
    var phoneNumber by remember { mutableStateOf(initialBeneficiary.phoneNumber) }
    var address by remember { mutableStateOf(initialBeneficiary.address) }
    var incomeSource by remember { mutableStateOf(initialBeneficiary.incomeSource) }
    var areaCode by remember { mutableStateOf(initialBeneficiary.areaCode) }
    var natureOfAddress by remember { mutableStateOf(initialBeneficiary.natureOfAddress) }
    var natureOfRent by remember { mutableStateOf(initialBeneficiary.natureOfRent ?: "") }
    var diseaseInability by remember { mutableStateOf(initialBeneficiary.diseaseInability ?: "") }
    var reasonForAid by remember { mutableStateOf(initialBeneficiary.reasonForAid) }
    var numberOfDependants by remember { mutableStateOf(if (initialBeneficiary.numberOfDependants == 0) "" else initialBeneficiary.numberOfDependants.toString()) }

    var isAreaDropDownExpanded by remember { mutableStateOf(false) }
    var isNatureDropDownExpanded by remember { mutableStateOf(false) }
    var isHeadGenderDropDownExpanded by remember { mutableStateOf(false) }
    
    val areaCodes = listOf("1 - Gate no 5", "2 - Azmi Nagar", "3 - Ambojwadi", "4 - Other")
    val natureOptions = listOf("Owned", "Rented")
    val genderOptions = listOf("Male", "Female", "Other")
    val relationOptions = listOf("Wife", "Son", "Daughter", "Father", "Mother", "Brother", "Sister", "Husband", "Other")

    val familyMembers = remember { mutableStateListOf<FamilyMember>().apply { addAll(initialBeneficiary.familyMembers) } }
    val familyMemberGenderExpanded = remember { mutableStateMapOf<Int, Boolean>() }
    val familyMemberRelationExpanded = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Beneficiary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = headName,
                    onValueChange = { headName = it },
                    label = { Text("Head Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = headAge,
                        onValueChange = { if (it.all { char -> char.isDigit() }) headAge = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = isHeadGenderDropDownExpanded,
                        onExpandedChange = { isHeadGenderDropDownExpanded = !isHeadGenderDropDownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = headGender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isHeadGenderDropDownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isHeadGenderDropDownExpanded,
                            onDismissRequest = { isHeadGenderDropDownExpanded = false }
                        ) {
                            genderOptions.forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(gender) },
                                    onClick = {
                                        headGender = gender
                                        isHeadGenderDropDownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = headOccupation,
                    onValueChange = { headOccupation = it },
                    label = { Text("Occupation") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = headEducation,
                    onValueChange = { headEducation = it },
                    label = { Text("Education") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                            phoneNumber = it
                        }
                    },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Address & Aid Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isAreaDropDownExpanded,
                    onExpandedChange = { isAreaDropDownExpanded = !isAreaDropDownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = areaCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Area Code") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAreaDropDownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isAreaDropDownExpanded,
                        onDismissRequest = { isAreaDropDownExpanded = false }
                    ) {
                        areaCodes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = {
                                    areaCode = code
                                    isAreaDropDownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isNatureDropDownExpanded,
                    onExpandedChange = { isNatureDropDownExpanded = !isNatureDropDownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = natureOfAddress,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nature of Address") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNatureDropDownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isNatureDropDownExpanded,
                        onDismissRequest = { isNatureDropDownExpanded = false }
                    ) {
                        natureOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    natureOfAddress = option
                                    isNatureDropDownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (natureOfAddress == "Rented") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = natureOfRent,
                        onValueChange = { natureOfRent = it },
                        label = { Text("Rent Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = incomeSource,
                    onValueChange = { incomeSource = it },
                    label = { Text("Income Source") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = diseaseInability,
                    onValueChange = { diseaseInability = it },
                    label = { Text("Disease/Inability") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reasonForAid,
                    onValueChange = { reasonForAid = it },
                    label = { Text("Reason for Aid") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = numberOfDependants,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            numberOfDependants = it
                            val newSize = it.toIntOrNull() ?: 0
                            while (familyMembers.size < newSize) {
                                familyMembers.add(FamilyMember("", "", 0, "", "", ""))
                            }
                            while (familyMembers.size > newSize) {
                                familyMembers.removeAt(familyMembers.size - 1)
                            }
                        }
                    },
                    label = { Text("Number of Dependants (Family Members)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Family Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        familyMembers.add(FamilyMember("", "", 0, "", "", ""))
                        numberOfDependants = familyMembers.size.toString()
                    }) {
                        Text("+ Add Member")
                    }
                }
            }

            itemsIndexed(familyMembers) { index, member ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Member ${index + 1} Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { 
                                familyMembers.removeAt(index)
                                val newSize = familyMembers.size
                                numberOfDependants = if (newSize == 0) "" else newSize.toString()
                            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Remove")
                            }
                        }
                        
                        val isRelExpanded = familyMemberRelationExpanded[index] ?: false
                        ExposedDropdownMenuBox(
                            expanded = isRelExpanded,
                            onExpandedChange = { familyMemberRelationExpanded[index] = !isRelExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = member.relation,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Relation with Head") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRelExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isRelExpanded,
                                onDismissRequest = { familyMemberRelationExpanded[index] = false }
                            ) {
                                relationOptions.forEach { rel ->
                                    DropdownMenuItem(
                                        text = { Text(rel) },
                                        onClick = {
                                            familyMembers[index] = member.copy(relation = rel)
                                            familyMemberRelationExpanded[index] = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = member.name,
                            onValueChange = { familyMembers[index] = member.copy(name = it) },
                            label = { Text("Member Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (member.age == 0) "" else member.age.toString(),
                                onValueChange = { if (it.all { char -> char.isDigit() }) familyMembers[index] = member.copy(age = it.toIntOrNull() ?: 0) },
                                label = { Text("Age") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            
                            val isExpanded = familyMemberGenderExpanded[index] ?: false
                            ExposedDropdownMenuBox(
                                expanded = isExpanded,
                                onExpandedChange = { familyMemberGenderExpanded[index] = !isExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = member.gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Gender") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isExpanded,
                                    onDismissRequest = { familyMemberGenderExpanded[index] = false }
                                ) {
                                    genderOptions.forEach { gender ->
                                        DropdownMenuItem(
                                            text = { Text(gender) },
                                            onClick = {
                                                familyMembers[index] = member.copy(gender = gender)
                                                familyMemberGenderExpanded[index] = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = member.occupation,
                            onValueChange = { familyMembers[index] = member.copy(occupation = it) },
                            label = { Text("Occupation") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = member.education,
                            onValueChange = { familyMembers[index] = member.copy(education = it) },
                            label = { Text("Education") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = member.diseaseInability ?: "",
                            onValueChange = { familyMembers[index] = member.copy(diseaseInability = it) },
                            label = { Text("Disease/Inability") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (state is OnboardingState.Loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            val updatedBeneficiary = initialBeneficiary.copy(
                                headName = headName,
                                headAge = headAge.toIntOrNull() ?: 0,
                                headGender = headGender,
                                headOccupation = headOccupation,
                                headEducation = headEducation,
                                phoneNumber = phoneNumber,
                                address = address,
                                incomeSource = incomeSource,
                                areaCode = areaCode,
                                natureOfAddress = natureOfAddress,
                                natureOfRent = natureOfRent,
                                diseaseInability = diseaseInability,
                                reasonForAid = reasonForAid,
                                numberOfDependants = numberOfDependants.toIntOrNull() ?: 0,
                                familyMembers = familyMembers.toList()
                            )
                            onSubmit(updatedBeneficiary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Changes")
                    }
                }

                if (state is OnboardingState.Error) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview
@Composable
fun EditBeneficiaryContentPreview() {
    MaterialTheme {
        EditBeneficiaryContent(
            initialBeneficiary = PreviewMocks.mockBeneficiary,
            state = OnboardingState.Idle,
            onBack = {},
            onSubmit = {}
        )
    }
}
