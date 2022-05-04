shared_context :uploaded_jpg do
  include_context :test_media_store
  before :each do
    visit '/media-service/uploads/'
    within "#uploads-page .form" do
      attach_file nil, "spec/support/files/AnonPhoto.jpg"
    end
    find_link('Original', wait: 10)
    click_link "Original"
  end
end
