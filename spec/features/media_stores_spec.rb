describe "Media Stores", type: :feature do
  let(:user) { create(:user, :with_system_admin_role) }

  before do
    sign_in
  end

  it "displays it ;)" do
    visit '/media-service/'

    find('.navbar .dropdown-toggle').click
    click_link 'Media-Stores'

    expect(page).to have_css('h2', text: 'Media-Stores')

    within '#stores-page' do
      expect(page).to have_css('table')
      within 'table' do
        expect(page).to have_css('tr', text: 'legacy-file-store filesystem')
        expect(page).to have_css('tr', text: 'database database')
      end
    end
  end

  it "navigates to store users page" do
    visit "/media-service/settings/stores/"

    click_link href: '/media-service/settings/stores/legacy-file-store/users/'

    expect(page).to have_css('h2', text: 'Media-Store legacy-file-store Users')

    page.assert_selector('table.users tbody tr', minimum: 5)

    fill_in 'term', with: 'ada'

    expect(page).to have_css('table.users tbody tr', count: 1)

    within '.direct-priority-component' do
      expect(page).to have_content '-'

      click_button 'Edit'
    end

    expect(page).to have_css('.modal')
    fill_in 'direct_priority', with: 5

    within('.modal') { click_button 'Save' }

    expect(page).to have_css('table.users tbody tr', count: 1)
    within '.combined-priority-component' do
      expect(page).to have_content '5'
    end
  end
end
